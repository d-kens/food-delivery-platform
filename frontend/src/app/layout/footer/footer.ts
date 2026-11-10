import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-footer',
  imports: [RouterLink],
  templateUrl: './footer.html',
})
export class Footer {
  protected readonly year = new Date().getFullYear();

  protected readonly columns = [
    { title: 'Company', links: ['About us', 'Careers', 'Blog', 'Press'] },
    { title: 'Partners', links: ['Add your restaurant', 'Become a rider', 'Keroma for business'] },
    { title: 'Support', links: ['Help centre', 'Contact us', 'Terms of service', 'Privacy policy'] },
  ];
}
