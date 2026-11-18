import { Component } from '@angular/core';
import { DeleteAccount } from '../delete-account/delete-account';
import { Password } from '../password/password';

@Component({
  selector: 'app-security',
  imports: [Password, DeleteAccount],
  template: `
    <app-password />
    <hr class="my-10 border-zinc-200" />
    <app-delete-account />
  `,
})
export class Security {}
