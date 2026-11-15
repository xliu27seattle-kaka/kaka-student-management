/** How each status is shown: a label and a badge colour. One place, so every page agrees. */
export type Tone = 'ok' | 'accent' | 'warn' | 'danger' | 'plain';

const STATUS: Record<string, { label: string; tone: Tone }> = {
  // accounts
  PENDING: { label: 'Pending', tone: 'warn' },
  ACTIVE: { label: 'Active', tone: 'ok' },
  REJECTED: { label: 'Rejected', tone: 'danger' },
  INACTIVE: { label: 'Inactive', tone: 'plain' },
  GRADUATED: { label: 'Graduated', tone: 'accent' },
  // semesters
  DRAFT: { label: 'Draft', tone: 'plain' },
  READY: { label: 'Enrollment open', tone: 'ok' },
  STARTED: { label: 'In session', tone: 'accent' },
  ENDED: { label: 'Ended', tone: 'plain' },
  // enrollments
  ENROLLED: { label: 'Enrolled', tone: 'ok' },
  DROPPED: { label: 'Dropped', tone: 'plain' },
  WITHDRAWN: { label: 'Withdrawn', tone: 'warn' },
  COMPLETED: { label: 'Completed', tone: 'accent' },
  // graduation
  APPROVED: { label: 'Approved', tone: 'ok' },
  // emails
  SENT: { label: 'Sent', tone: 'ok' },
  FAILED: { label: 'Failed', tone: 'danger' },
};

export function statusLabel(status: string): string {
  return STATUS[status]?.label ?? status;
}

export function statusTone(status: string): Tone {
  return STATUS[status]?.tone ?? 'plain';
}
