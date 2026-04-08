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

## 0.5.1 (04-07-2026)

### Features & Enhancements

- **AI Integration:** Added a button and backend logic to suggest the appropriate office using Gemini models [#111](https://github.com/CusmaLinux/svu/pull/111), [#112](https://github.com/CusmaLinux/svu/pull/112).
- **User Features & Management:**
  - Introduced a calendar feature to handle special dates [#108](https://github.com/CusmaLinux/svu/pull/108).
  - Added password change functionality for management users [#106](https://github.com/CusmaLinux/svu/pull/106).
  - Implemented the logic and public UI modal for the new Satisfaction Survey [#114](https://github.com/CusmaLinux/svu/pull/114), [#117](https://github.com/CusmaLinux/svu/pull/117).
  - Created a `simple-link` component to properly restrict access to the office page [#131](https://github.com/CusmaLinux/svu/pull/131).
- **Search & Filters:**
  - Added a search filter to the responses page [#105](https://github.com/CusmaLinux/svu/pull/105).
  - Added a search filter to the attachments panel [#102](https://github.com/CusmaLinux/svu/pull/102).
- **UI & Design:**
  - Increased the number of users displayed in offices and improved the response page UI [#107](https://github.com/CusmaLinux/svu/pull/107).
  - Added a list of attachment files to the response detail view [#130](https://github.com/CusmaLinux/svu/pull/130).
  - Hidden the bottom button to create a new attached file where appropriate [#129](https://github.com/CusmaLinux/svu/pull/129).
  - Improved the `informe-pqrs-update` component [#139](https://github.com/CusmaLinux/svu/pull/139).
- **DevOps, Config & Refactoring:**
  - Refactored the continuous deployment pipeline to connect via an action runner to the new VM [#144](https://github.com/CusmaLinux/svu/pull/144).
  - Refactored the `extractTextFromImage` function to use a trained tessdata source.
  - Updated the README with the complete project setup instructions [#141](https://github.com/CusmaLinux/svu/pull/141).
  - Implemented minor improvements to the configuration files in Spring Boot and Vue.js [#145](https://github.com/CusmaLinux/svu/pull/145).
  - Applied various minimal improvements and changes across the codebase [#103](https://github.com/CusmaLinux/svu/pull/103), [#116](https://github.com/CusmaLinux/svu/pull/116).

### Fixes

- Corrected an issue with the creation of PQRS by authenticated users [#104](https://github.com/CusmaLinux/svu/pull/104).
- Fixed an error that occurred when removing an office [#128](https://github.com/CusmaLinux/svu/pull/128).
- Fixed a bug related to the "activated" field in the form when creating a user [#133](https://github.com/CusmaLinux/svu/pull/133).
- Fixed failing frontend tests [#137](https://github.com/CusmaLinux/svu/pull/137).
- Fixed failing unit and integration tests in the backend [#135](https://github.com/CusmaLinux/svu/pull/135).
