import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding, withViewTransitions } from '@angular/router';
import { routes } from './app.routes';
import { tagViewTransition } from './core/motion/transitions';
import { authInterceptor } from './core/http/auth-interceptor';
import { errorInterceptor } from './core/http/error-interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      withComponentInputBinding(),
      // Animated page changes (styles/motion.scss); skipped on the very first load.
      withViewTransitions({
        skipInitialTransition: true,
        onViewTransitionCreated: tagViewTransition,
      }),
    ),
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
  ],
};
