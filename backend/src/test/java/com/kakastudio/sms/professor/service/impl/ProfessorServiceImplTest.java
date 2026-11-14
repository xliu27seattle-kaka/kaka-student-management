package com.kakastudio.sms.professor.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kakastudio.sms.common.error.ApiException;
import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.notification.email.dto.EmailMessage;
import com.kakastudio.sms.notification.email.service.EmailOutboxService;
import com.kakastudio.sms.notification.email.service.impl.HtmlEmailComposer;
import com.kakastudio.sms.professor.domain.AwardEntry;
import com.kakastudio.sms.professor.domain.ExperienceEntry;
import com.kakastudio.sms.professor.domain.Professor;
import com.kakastudio.sms.professor.dto.AwardDto;
import com.kakastudio.sms.professor.dto.ExperienceDto;
import com.kakastudio.sms.professor.dto.ProfessorCreateRequest;
import com.kakastudio.sms.professor.dto.ProfessorProfileRequest;
import com.kakastudio.sms.professor.dto.ProfessorProfileResponse;
import com.kakastudio.sms.professor.dto.ProfessorResponse;
import com.kakastudio.sms.professor.mapper.ProfessorMapper;
import com.kakastudio.sms.professor.repository.ProfessorRepository;
import com.kakastudio.sms.professor.service.ProfessorNumberGenerator;
import com.kakastudio.sms.security.dto.CurrentUser;
import com.kakastudio.sms.security.service.CurrentUserProvider;
import com.kakastudio.sms.testsupport.Users;
import com.kakastudio.sms.user.domain.Role;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;
import com.kakastudio.sms.user.repository.UserRepository;
import com.kakastudio.sms.user.service.TemporaryPasswordGenerator;
import com.kakastudio.sms.user.service.impl.UserAccountServiceImpl;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class ProfessorServiceImplTest {

  private final ProfessorRepository professors = mock(ProfessorRepository.class);
  private final UserRepository users = mock(UserRepository.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private final ProfessorNumberGenerator numbers = mock(ProfessorNumberGenerator.class);
  private final TemporaryPasswordGenerator temporaryPasswords =
      mock(TemporaryPasswordGenerator.class);
  private final EmailOutboxService outbox = mock(EmailOutboxService.class);
  private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
  private ProfessorServiceImpl service;

  @BeforeEach
  void setUp() {
    when(passwordEncoder.encode(any())).thenAnswer(call -> "encoded:" + call.getArgument(0));
    when(users.save(any())).thenAnswer(call -> call.getArgument(0));
    when(professors.save(any())).thenAnswer(call -> call.getArgument(0));
    when(numbers.next()).thenReturn("P-0001");
    when(temporaryPasswords.generate()).thenReturn("Temp1234abcd");
    service =
        new ProfessorServiceImpl(
            professors,
            new UserAccountServiceImpl(users, passwordEncoder),
            numbers,
            temporaryPasswords,
            new HtmlEmailComposer(),
            outbox,
            new ProfessorMapper(),
            currentUser);
  }

  private Professor stored(Long id, UserStatus status) {
    User user = Users.user(200L + id, "amara.okafor@faculty.example", Role.PROFESSOR, status);
    user.setFirstName("Amara");
    user.setLastName("Okafor");
    Professor professor = new Professor(user, "P-0001");
    professor.setId(id);
    professor.setTitle("Associate Professor of Computer Science");
    professor.getExpertise().addAll(List.of("Old topic"));
    professor.getAwards().add(new AwardEntry("Old award", 2010));
    when(professors.findById(id)).thenReturn(Optional.of(professor));
    when(professors.findByUserId(user.getId())).thenReturn(Optional.of(professor));
    return professor;
  }

  private ApiException failure(Runnable action) {
    return catchThrowableOfType(ApiException.class, action::run);
  }

  private static ProfessorProfileRequest profile() {
    return new ProfessorProfileRequest(
        "Associate Professor of Computer Science",
        "I teach databases.",
        List.of("Databases", "Distributed systems"),
        List.of(new ExperienceDto("Senior Engineer", "Northwind Data", "2012 to 2018")),
        List.of(new AwardDto("Teaching Excellence Award", 2023)));
  }

  @Test
  void createMakesAnActiveProfessorAndEmailsTheTemporaryPassword() {
    when(users.findByEmail("amara.okafor@faculty.example")).thenReturn(Optional.empty());

    ProfessorResponse response =
        service.create(
            new ProfessorCreateRequest(
                " Amara.Okafor@Faculty.Example", "Amara", "Okafor", profile()));

    assertThat(response.professorNo()).isEqualTo("P-0001");
    assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
    assertThat(response.title()).isEqualTo("Associate Professor of Computer Science");
    ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
    verify(users).save(savedUser.capture());
    assertThat(savedUser.getValue().getEmail()).isEqualTo("amara.okafor@faculty.example");
    assertThat(savedUser.getValue().getRole()).isEqualTo(Role.PROFESSOR);
    assertThat(savedUser.getValue().getPasswordHash()).isEqualTo("encoded:Temp1234abcd");
    ArgumentCaptor<Professor> savedProfessor = ArgumentCaptor.forClass(Professor.class);
    verify(professors).save(savedProfessor.capture());
    assertThat(savedProfessor.getValue().getExpertise())
        .containsExactly("Databases", "Distributed systems");
    ArgumentCaptor<EmailMessage> email = ArgumentCaptor.forClass(EmailMessage.class);
    verify(outbox).enqueue(email.capture());
    assertThat(email.getValue().subject()).isEqualTo("Your Kaka Student Management account");
    assertThat(email.getValue().html()).contains("Temp1234abcd");
  }

  @Test
  void createWithATakenEmailIsAConflict() {
    when(users.findByEmail("amara.okafor@faculty.example"))
        .thenReturn(Optional.of(Users.activeStudent(7L)));

    ApiException thrown =
        failure(() ->
            service.create(
                new ProfessorCreateRequest(
                    "amara.okafor@faculty.example", "Amara", "Okafor", null)));

    assertThat(thrown.status()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(thrown.code()).isEqualTo(ErrorCode.EMAIL_TAKEN);
    verify(professors, never()).save(any());
  }

  @Test
  void createWithoutAProfileStartsEmpty() {
    when(users.findByEmail(any())).thenReturn(Optional.empty());

    service.create(new ProfessorCreateRequest("new.prof@faculty.example", "New", "Prof", null));

    ArgumentCaptor<Professor> saved = ArgumentCaptor.forClass(Professor.class);
    verify(professors).save(saved.capture());
    assertThat(saved.getValue().getExpertise()).isEmpty();
    assertThat(saved.getValue().getAwards()).isEmpty();
  }

  @Test
  void updateMyProfileReplacesTheListsInOrder() {
    Professor professor = stored(4L, UserStatus.ACTIVE);
    when(currentUser.require())
        .thenReturn(new CurrentUser(204L, "amara.okafor@faculty.example", Role.PROFESSOR));

    ProfessorProfileResponse response = service.updateMyProfile(profile());

    assertThat(professor.getExpertise()).containsExactly("Databases", "Distributed systems");
    assertThat(professor.getAwards())
        .containsExactly(new AwardEntry("Teaching Excellence Award", 2023));
    assertThat(professor.getExperience())
        .containsExactly(new ExperienceEntry("Senior Engineer", "Northwind Data", "2012 to 2018"));
    assertThat(professor.getBio()).isEqualTo("I teach databases.");
    assertThat(response.expertise()).containsExactly("Databases", "Distributed systems");
    verify(professors).findByUserId(204L);
  }

  @Test
  void profileOfAnInactiveProfessorIsNotFound() {
    stored(4L, UserStatus.INACTIVE);

    ApiException thrown = failure(() -> service.profile(4L));

    assertThat(thrown.status()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(thrown.code()).isEqualTo(ErrorCode.PROFESSOR_NOT_FOUND);
  }

  @Test
  void theProfilePageNeverCarriesTheEmailAddress() {
    assertThat(
            Arrays.stream(ProfessorProfileResponse.class.getRecordComponents())
                .map(component -> component.getName().toLowerCase()))
        .noneMatch(name -> name.contains("email"));
  }

  @Test
  void profileShowsThePhotoUrlOnlyWhenThereIsAPhoto() {
    Professor professor = stored(4L, UserStatus.ACTIVE);

    assertThat(service.profile(4L).photoUrl()).isNull();
    professor.setPhotoPath("amara-okafor.webp");
    assertThat(service.profile(4L).photoUrl()).isEqualTo("/assets/professors/amara-okafor.webp");
  }

  @Test
  void directoryMapsTheActiveProfessors() {
    Professor professor = stored(4L, UserStatus.ACTIVE);
    when(professors.findAllByUserStatusOrderByUserLastNameAscUserFirstNameAsc(UserStatus.ACTIVE))
        .thenReturn(List.of(professor));

    List<ProfessorProfileResponse> directory = service.directory();

    assertThat(directory).hasSize(1);
    assertThat(directory.get(0).lastName()).isEqualTo("Okafor");
  }

  @Test
  void deactivatingTwiceIsAConflict() {
    stored(4L, UserStatus.ACTIVE);

    assertThat(service.deactivate(4L).status()).isEqualTo(UserStatus.INACTIVE);
    assertThat(failure(() -> service.deactivate(4L)).code())
        .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
  }
}
