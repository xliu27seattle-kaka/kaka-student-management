import { inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';
import { ConfirmStore } from '../ui/confirm-store';

/** A page with a form says whether leaving now would lose what was typed. */
export interface HasUnsavedChanges {
  hasUnsavedChanges(): boolean;
}

export const UNSAVED_CHANGES_QUESTION = {
  title: 'Leave without saving?',
  message: 'You have changes that are not saved. If you leave now they will be lost.',
  confirmLabel: 'Leave',
  cancelLabel: 'Stay',
  danger: true,
};

/** Asks before leaving a route whose form has unsaved changes. */
export const unsavedChangesGuard: CanDeactivateFn<HasUnsavedChanges> = (component) =>
  component.hasUnsavedChanges() ? inject(ConfirmStore).ask(UNSAVED_CHANGES_QUESTION) : true;
