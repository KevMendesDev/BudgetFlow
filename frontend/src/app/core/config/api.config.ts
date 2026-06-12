import { environment } from '../../../environments/environment';
import { getRuntimeConfig } from './runtime-config';

export const API_BASE_URL = getRuntimeConfig().apiBaseUrl ?? environment.apiBaseUrl;
