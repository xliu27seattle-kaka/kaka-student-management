package com.kakastudio.sms.professor.service.impl;

import com.kakastudio.sms.common.error.ConflictException;
import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.common.error.NotFoundException;
import com.kakastudio.sms.common.paging.PageRequests;
import com.kakastudio.sms.common.paging.PageResponse;
import com.kakastudio.sms.notification.email.service.EmailComposer;
import com.kakastudio.sms.notification.message.service.SystemNotices;
import com.kakastudio.sms.professor.domain.Professor;
import com.kakastudio.sms.professor.dto.ProfessorCreateRequest;
import com.kakastudio.sms.professor.dto.ProfessorProfileRequest;
import com.kakastudio.sms.professor.dto.ProfessorProfileResponse;
import com.kakastudio.sms.professor.dto.ProfessorResponse;
import com.kakastudio.sms.professor.dto.ProfessorUpdateRequest;
import com.kakastudio.sms.professor.mapper.ProfessorMapper;
import com.kakastudio.sms.professor.repository.ProfessorRepository;
import com.kakastudio.sms.professor.repository.ProfessorSpecifications;
import com.kakastudio.sms.professor.service.ProfessorAdminService;
import com.kakastudio.sms.professor.service.ProfessorDirectoryService;
import com.kakastudio.sms.professor.service.ProfessorNumberGenerator;
import com.kakastudio.sms.professor.service.ProfessorProfileService;
import com.kakastudio.sms.section.domain.CourseSection;
import com.kakastudio.sms.section.repository.CourseSectionRepository;
import com.kakastudio.sms.security.service.CurrentUserProvider;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import com.kakastudio.sms.student.service.impl.StudentServiceImpl;
import com.kakastudio.sms.user.domain.Role;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;
import com.kakastudio.sms.user.service.NewAccount;
import com.kakastudio.sms.user.service.TemporaryPasswordGenerator;
import com.kakastudio.sms.user.service.UserAccountService;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Everything about professors. It is used through three narrow interfaces, one per kind of caller
 * (administrators, readers of the directory, the professor themself), so a controller only sees the
 * operations meant for it.
 */
@Service
public class ProfessorServiceImpl
    implements ProfessorAdminService, ProfessorDirectoryService, ProfessorProfileService {

  private final ProfessorRepository professors;
  private final UserAccountService accounts;
  private final ProfessorNumberGenerator numbers;
  private final TemporaryPasswordGenerator temporaryPasswords;
  private final EmailComposer emails;
  private final SystemNotices notices;
  private final ProfessorMapper mapper;
  private final CurrentUserProvider currentUser;
  private final CourseSectionRepository sections;

  public ProfessorServiceImpl(
      ProfessorRepository professors,
      UserAccountService accounts,
      ProfessorNumberGenerator numbers,
      TemporaryPasswordGenerator temporaryPasswords,
      EmailComposer emails,
      SystemNotices notices,
      ProfessorMapper mapper,
      CurrentUserProvider currentUser,
      CourseSectionRepository sections) {
    this.professors = professors;
    this.accounts = accounts;
    this.numbers = numbers;
    this.temporaryPasswords = temporaryPasswords;
    this.emails = emails;
    this.notices = notices;
    this.mapper = mapper;
    this.currentUser = currentUser;
    this.sections = sections;
  }

  // ---- administrators ----

  @Override
  @Transactional(readOnly = true)
  public PageResponse<ProfessorResponse> list(String query, int page, int size) {
    return PageResponse.of(
        professors.findAll(
            ProfessorSpecifications.matching(query), PageRequests.newestFirst(page, size)),
        mapper::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public ProfessorResponse get(Long id) {
    return mapper.toResponse(find(id));
  }

  @Override
  @Transactional
  public ProfessorResponse create(ProfessorCreateRequest request) {
    String temporaryPassword = temporaryPasswords.generate();
    User user =
        accounts.create(
            new NewAccount(
                request.email(),
                request.firstName(),
                request.lastName(),
                temporaryPassword,
                Role.PROFESSOR,
                UserStatus.ACTIVE));
    Professor professor = new Professor(user, numbers.next());
    if (request.profile() != null) {
      mapper.applyProfile(request.profile(), professor);
    }
    professors.save(professor);
    notices.send(
        user,
        emails.accountCreated(user, temporaryPassword),
        StudentServiceImpl.ACCOUNT_CREATED_NOTE);
    return mapper.toResponse(professor);
  }

  @Override
  @Transactional
  public ProfessorResponse update(Long id, ProfessorUpdateRequest request) {
    Professor professor = find(id);
    accounts.changeDetails(
        professor.getUser(), request.email(), request.firstName(), request.lastName());
    return mapper.toResponse(professor);
  }

  @Override
  @Transactional
  public ProfessorResponse deactivate(Long id) {
    List<String> teaching =
        sections
            .findByProfessorAndSemesterStatuses(
                id, EnumSet.of(SemesterStatus.READY, SemesterStatus.STARTED))
            .stream()
            .map(CourseSection::code)
            .toList();
    if (!teaching.isEmpty()) {
      throw new ConflictException(
          ErrorCode.HAS_ACTIVE_COMMITMENTS,
          "Teaches "
              + String.join(", ", teaching)
              + " in an open or running semester. The account can be deactivated once that"
              + " semester has ended (a section's professor is fixed once enrollment opens).");
    }
    return mapper.toResponse(changeStatus(id, UserStatus.ACTIVE, UserStatus.INACTIVE));
  }

  @Override
  @Transactional
  public ProfessorResponse reactivate(Long id) {
    return mapper.toResponse(changeStatus(id, UserStatus.INACTIVE, UserStatus.ACTIVE));
  }

  @Override
  @Transactional
  public void delete(Long id) {
    Professor professor = find(id);
    if (sections.existsByProfessorId(id)) {
      throw new ConflictException(
          ErrorCode.HAS_HISTORY,
          "This professor has taught sections, so the record stays. Deactivate the account instead.");
    }
    professors.delete(professor);
    accounts.delete(professor.getUser());
  }

  // ---- every signed-in user ----

  @Override
  @Transactional(readOnly = true)
  public List<ProfessorProfileResponse> directory() {
    return professors
        .findAllByUserStatusOrderByUserLastNameAscUserFirstNameAsc(UserStatus.ACTIVE)
        .stream()
        .map(mapper::toProfile)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public ProfessorProfileResponse profile(Long id) {
    Professor professor = find(id);
    if (professor.getUser().getStatus() != UserStatus.ACTIVE) {
      throw notFound(); // a deactivated professor has no public page
    }
    return mapper.toProfile(professor);
  }

  // ---- the professor themself ----

  @Override
  @Transactional(readOnly = true)
  public ProfessorProfileResponse myProfile() {
    return mapper.toProfile(mine());
  }

  @Override
  @Transactional
  public ProfessorProfileResponse updateMyProfile(ProfessorProfileRequest request) {
    Professor professor = mine();
    mapper.applyProfile(request, professor);
    return mapper.toProfile(professor);
  }

  /** The professor row of whoever is signed in. Never taken from a request parameter. */
  private Professor mine() {
    return professors.findByUserId(currentUser.require().id()).orElseThrow(this::notFound);
  }

  private Professor changeStatus(Long id, UserStatus from, UserStatus to) {
    Professor professor = find(id);
    accounts.changeStatus(professor.getUser(), from, to);
    return professor;
  }

  private Professor find(Long id) {
    return professors.findById(id).orElseThrow(this::notFound);
  }

  private NotFoundException notFound() {
    return new NotFoundException(ErrorCode.PROFESSOR_NOT_FOUND, "Professor not found");
  }
}
