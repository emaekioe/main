import { CanDeactivateFn } from '@angular/router';
import { ConfigurationComponent } from '../pages/configuration/configuration.component';

export const unsavedChangesGuard: CanDeactivateFn<ConfigurationComponent> = (component) => {
  if (!component.hasUnsavedChanges) {
    return true;
  }

  component.showLeaveWarning = true;

  return new Promise<boolean>((resolve) => {
    component.onLeaveDecision = resolve;
  });
};
