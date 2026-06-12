import { HttpBackend, HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class RawHttpService {
  readonly http = new HttpClient(inject(HttpBackend));
}
