# CHANGELOG.md

## 0.1.0 (04-27-2025)

Features:

- Add the contributing.md file [#3](https://github.com/CusmaLinux/svu/pull/3)
- Adding additional initial data for offices, users and authorities [#6](https://github.com/CusmaLinux/svu/pull/6)
- Add the continuous deployment pipeline [#18](https://github.com/CusmaLinux/svu/pull/18)

## 0.2.0 (05-25-2025)

Features:

- Fix basic functionality in attached files section and save with UUID in file system [#20](https://github.com/CusmaLinux/svu/pull/20)
- Filter the entities by type of user in the dashboard [#21](https://github.com/CusmaLinux/svu/pull/21)
- Add default fields for pqrs creation [#22](https://github.com/CusmaLinux/svu/pull/22)
- List the active PQRS for the admin and the functionary [#27](https://github.com/CusmaLinux/svu/pull/27)
- Add SSE Notifications for pqrs due date remender event [#32](https://github.com/CusmaLinux/svu/pull/32)
- Modify view to update PQRS [#33](https://github.com/CusmaLinux/svu/pull/33)
- Add the resolve button to pqrs detail in functionary view [#37](https://github.com/CusmaLinux/svu/pull/37)
- Create a security button and modal window to close pqrs [#38](https://github.com/CusmaLinux/svu/pull/38)
- Highlight deadline [#39](https://github.com/CusmaLinux/svu/pull/39)

## 0.3.0 (07-01-2025)

Features:

- Add the anonymous users workflow
- Refactor the SSE notifications to made extensible
- Changes in the PQRS model to handle better the registry
- Reuse the root path for different views in base to authorities
- Show the complete view of pqrs + response + attached files in a single page
- Improve the UI of different views .i.e pqrs and informs
- Configure the SMTP server for gmail provider
- Add the basic setup for sonar cloud
- Add the generation of reports in .xlsx format to inform-pqrs from the API
- Show the chart for the inform-pqrs detail in the client

## 0.4.0 (07-16-2025)

### Features & Enhancements

- **Security:** Added reCAPTCHA v3 service to protect public forms from automated bots (#98).
- **Permissions:** Implemented fine-grained user permissions for detailed control over component access (#91).
- **Notifications:**
  - Improved the user interface and experience of the notifications page (#99).
  - Users now receive a notification when a PQRSD (requirement) is assigned to them (#89).
- **Search & Forms:**
  - Enhanced PQRS search to allow role-based filtering by office (#87).
  - Made minor improvements to the usability of forms and the search for key entities (#97).
- **UI & Design:** Introduced a new prototype for the application logo and footer (#96).
- **Workflows:** Improved the internal workflow for the "office" domain (#94).
- **Performance & Refactoring:** Refactored the notifications feature and improved application performance on GCP (#85).

### Fixes

- Fixed a bug that prevented images from loading correctly (#82).
- Reverted a recent change related to the App Password feature to resolve an issue (#83).
