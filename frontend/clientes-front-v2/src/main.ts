import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';
import { environment } from './environments/environment';

bootstrapApplication(AppComponent, appConfig)
  .then(() => {
    console.log('[App] Aplicação iniciada');
    if ('serviceWorker' in navigator && environment.production) {
      navigator.serviceWorker.register('/sw.js').catch(() => undefined);
    }
  })
  .catch(err => console.error('[App] Erro ao iniciar:', err));
