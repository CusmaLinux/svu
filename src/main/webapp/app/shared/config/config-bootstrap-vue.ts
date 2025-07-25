import {
  BAlert,
  BBadge,
  BButton,
  BCollapse,
  BDropdown,
  BDropdownItem,
  BForm,
  BFormCheckbox,
  BFormDatepicker,
  BFormGroup,
  BFormInput,
  BInputGroup,
  BInputGroupPrepend,
  BLink,
  BModal,
  BNavItem,
  BNavItemDropdown,
  BNavbar,
  BNavbarBrand,
  BNavbarNav,
  BNavbarToggle,
  BPagination,
  BProgress,
  BProgressBar,
  BListGroup,
  BListGroupItem,
  BDropdownText,
  BDropdownDivider,
  BSpinner,
  BImg,
  BFormTextarea,
  BFormSelect,
  BFormInvalidFeedback,
  ToastPlugin,
  IconsPlugin,
  VBModal,
  VBTooltip,
  LayoutPlugin,
  CardPlugin,
  CollapsePlugin,
  TablePlugin,
} from 'bootstrap-vue';

import Multiselect from 'vue-multiselect';

export function initBootstrapVue(vue: any) {
  vue.use(ToastPlugin);
  vue.use(IconsPlugin);
  vue.use(LayoutPlugin);
  vue.use(CardPlugin);
  vue.use(CollapsePlugin);
  vue.use(TablePlugin);

  vue.component('b-badge', BBadge);
  vue.component('b-dropdown', BDropdown);
  vue.component('b-dropdown-item', BDropdownItem);
  vue.component('b-link', BLink);
  vue.component('b-alert', BAlert);
  vue.component('b-button', BButton);
  vue.component('b-navbar', BNavbar);
  vue.component('b-navbar-nav', BNavbarNav);
  vue.component('b-navbar-brand', BNavbarBrand);
  vue.component('b-navbar-toggle', BNavbarToggle);
  vue.component('b-pagination', BPagination);
  vue.component('b-progress', BProgress);
  vue.component('b-progress-bar', BProgressBar);
  vue.component('b-form', BForm);
  vue.component('b-form-input', BFormInput);
  vue.component('b-form-group', BFormGroup);
  vue.component('b-form-checkbox', BFormCheckbox);
  vue.component('b-collapse', BCollapse);
  vue.component('b-nav-item', BNavItem);
  vue.component('b-nav-item-dropdown', BNavItemDropdown);
  vue.component('b-modal', BModal);
  vue.directive('b-modal', VBModal);
  vue.component('b-form-datepicker', BFormDatepicker);
  vue.component('b-input-group', BInputGroup);
  vue.component('b-input-group-prepend', BInputGroupPrepend);
  vue.component('b-list-group', BListGroup);
  vue.component('b-list-group-item', BListGroupItem);
  vue.component('b-dropdown-text', BDropdownText);
  vue.component('b-dropdown-divider', BDropdownDivider);
  vue.component('b-spinner', BSpinner);
  vue.directive('b-tooltip', VBTooltip);
  vue.component('b-img', BImg);
  vue.component('v-multiselect', Multiselect);
  vue.component('b-form-textarea', BFormTextarea);
  vue.component('b-form-select', BFormSelect);
  vue.component('b-form-invalid-feedback', BFormInvalidFeedback);
}
