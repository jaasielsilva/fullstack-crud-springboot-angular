import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, appConfig)
  .then(() => console.log('[App] Aplicação iniciada'))
  .catch(err => console.error('[App] Erro ao iniciar:', err));
