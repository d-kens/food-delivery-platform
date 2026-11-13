import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { userResponse } from '../../../testing/users';
import { UserList } from './user-list';

describe('UserList', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;
  let router: Router;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'admin/users', component: UserList }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => http.verify());

  const page = () => harness.routeNativeElement!;
  const listRequest = () => http.expectOne((r) => r.url === '/users');
  const flushPage = async (
    request: TestRequest,
    content = [userResponse()],
    extra: object = {},
  ) => {
    request.flush({
      content,
      page: 0,
      size: 20,
      totalElements: content.length,
      totalPages: 1,
      ...extra,
    });
    await harness.fixture.whenStable();
  };

  it('should load the first page and show each user with their status', async () => {
    await harness.navigateByUrl('/admin/users');
    const request = listRequest();
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('20');

    await flushPage(request, [
      userResponse(),
      userResponse({ userId: 'user-2', firstName: 'John', status: 'SUSPENDED' }),
    ]);

    expect(page().textContent).toContain('Jane Doe');
    expect(page().textContent).toContain('John Doe');
    expect(page().textContent).toContain('Suspended');
    expect(page().querySelector('a[href="/admin/users/user-2"]')).toBeTruthy();
  });

  it('should read filters from the URL and send them to the API', async () => {
    await harness.navigateByUrl('/admin/users?status=DELETED&role=ADMIN&search=jane&page=2');
    const request = listRequest();

    expect(request.request.params.get('status')).toBe('DELETED');
    expect(request.request.params.get('role')).toBe('ADMIN');
    expect(request.request.params.get('search')).toBe('jane');
    expect(request.request.params.get('page')).toBe('2');
    await flushPage(request);

    expect(page().querySelector<HTMLSelectElement>('#status')!.value).toBe('DELETED');
    expect(page().querySelector<HTMLInputElement>('#search')!.value).toBe('jane');
  });

  it('should ignore invalid filter values in the URL', async () => {
    await harness.navigateByUrl('/admin/users?status=BOGUS&page=-3');
    const request = listRequest();

    expect(request.request.params.has('status')).toBe(false);
    expect(request.request.params.get('page')).toBe('0');
    await flushPage(request);
  });

  it('should put a changed filter in the URL and go back to the first page', async () => {
    await harness.navigateByUrl('/admin/users?page=3');
    await flushPage(listRequest());

    const select = page().querySelector<HTMLSelectElement>('#status')!;
    select.value = 'SUSPENDED';
    select.dispatchEvent(new Event('change'));
    await harness.fixture.whenStable();

    expect(router.url).toBe('/admin/users?status=SUSPENDED');
    await flushPage(listRequest());
  });

  it('should search on submit', async () => {
    await harness.navigateByUrl('/admin/users');
    await flushPage(listRequest());

    const input = page().querySelector<HTMLInputElement>('#search')!;
    input.value = '  doe ';
    input.form!.dispatchEvent(new Event('submit'));
    await harness.fixture.whenStable();

    expect(router.url).toBe('/admin/users?search=doe');
    await flushPage(listRequest());
  });

  it('should page through results', async () => {
    await harness.navigateByUrl('/admin/users');
    await flushPage(listRequest(), [userResponse()], { totalElements: 41, totalPages: 3 });

    expect(page().textContent).toContain('Page 1 of 3');
    const next = [...page().querySelectorAll('button')].find(
      (b) => b.textContent?.trim() === 'Next',
    )!;
    next.click();
    await harness.fixture.whenStable();

    expect(router.url).toBe('/admin/users?page=1');
    await flushPage(listRequest());
  });

  it('should show an empty state', async () => {
    await harness.navigateByUrl('/admin/users');
    await flushPage(listRequest(), []);

    expect(page().textContent).toContain('No users match these filters.');
  });

  it('should show an error and retry', async () => {
    await harness.navigateByUrl('/admin/users');
    listRequest().flush(null, { status: 500, statusText: 'Server Error' });
    await harness.fixture.whenStable();

    expect(page().textContent).toContain("Couldn't load users.");
    [...page().querySelectorAll('button')]
      .find((b) => b.textContent?.includes('Try again'))!
      .click();
    await harness.fixture.whenStable();

    await flushPage(listRequest());
    expect(page().textContent).toContain('Jane Doe');
  });
});
