import { NgOptimizedImage } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

/** Login and register: focused form on the left, food photo on large screens. */
@Component({
  selector: 'app-auth-layout',
  imports: [RouterOutlet, RouterLink, NgOptimizedImage],
  templateUrl: './auth-layout.html',
})
export class AuthLayout {}
