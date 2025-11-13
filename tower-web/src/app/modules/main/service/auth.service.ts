/*
 * Copyright (c) 2019, Seqera Labs.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is "Incompatible With Secondary Licenses", as
 * defined by the Mozilla Public License, v. 2.0.
 */
import { Injectable } from '@angular/core';
import {BehaviorSubject, Observable} from "rxjs";
import {map, tap} from "rxjs/operators";
import {User} from "../entity/user/user";
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {environment} from "src/environments/environment";
import {DescribeUserResponse, UserData} from '../entity/user/user-data';
import {AccessGateResponse} from "../entity/gate";
import {Router} from "@angular/router";
import {NotificationService} from './notification.service';

const authEndpointUrl = `${environment.apiUrl}/login`;
const userEndpointUrl = `${environment.apiUrl}/user`;
const gateEndpointUrl = `${environment.apiUrl}/gate`;
const JWT_COOKIE = 'JWT';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  user$: Observable<User>;

  private userSubject: BehaviorSubject<User>;

  constructor(private http: HttpClient,
              private router: Router,
              private notificationService: NotificationService) {
    // Single-user mode: auto-login with default user
    const defaultUser = this.getOrCreateDefaultUser();
    this.userSubject = new BehaviorSubject(defaultUser);
    this.user$ = this.userSubject.asObservable();
  }

  private getOrCreateDefaultUser(): User {
    // Check if we have a persisted user
    let user = this.getPersistedUser();
    if (!user) {
      // Create a default user for single-user mode
      const defaultUserData = {
        id: 1,
        userName: 'tower-user',
        email: 'user@tower.local',
        firstName: 'Tower',
        lastName: 'User',
        trusted: true,
        organization: '',
        description: '',
        notification: false,
        avatar: ''
      };
      user = new User(defaultUserData);
      this.persistUser(user);
    }
    return user;
  }

  get isUserAuthenticated(): boolean {
    return (this.currentUser != null);
  }

  get currentUser(): User {
    return this.userSubject.value;
  }

  get authEndpointUrl(): string {
    return authEndpointUrl;
  }

  setAuthorizedUser(): void {
    // In single-user mode, just use the default user
    const defaultUser = this.getOrCreateDefaultUser();
    this.setAuthUser(defaultUser);
    this.router.navigate(['']);
  }

  private setAuthUser(user: User): void {
    this.persistUser(user);
    this.userSubject.next(user);
  }

  access(email: string, captcha: string): Observable<AccessGateResponse> {
    return this.http.post<AccessGateResponse>(`${gateEndpointUrl}/access`, {email, captcha});
  }

  update(user: User): Observable<string> {
    return this.http.post(`${userEndpointUrl}/update`, user.data, {responseType: "text"}).pipe(
      map((message: string) => message),
      tap( () => this.setAuthUser(user)),
    );
  }

  delete(): Observable<string> {
    return this.http.delete(`${userEndpointUrl}/delete`, {responseType: "text"}).pipe(
      map((message: string) => message)
    );
  }

  logoutAndGoHome() {
    this.logout();
    this.router.navigate(['/']);
  }

  logout(): void {
    this.removeUser();
    this.deleteCookie(JWT_COOKIE);
    this.userSubject.next(null);
  }

  private deleteCookie(cookieName: string) {
    document.cookie = cookieName + '=; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
  }

  private persistUser(user: User): void {
    localStorage.setItem('user', JSON.stringify(user.data));
  }

  private getPersistedUser(): User {
    const userData: UserData = JSON.parse(localStorage.getItem('user')) as UserData;
    return (userData ? new User(userData) : null);
  }

  private removeUser(): void {
    localStorage.removeItem('user');
  }
}
