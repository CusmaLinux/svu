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

  vue.component('BBadge', BBadge);
  vue.component('BDropdown', BDropdown);
  vue.component('BDropdownItem', BDropdownItem);
  vue.component('BLink', BLink);
  vue.component('BAlert', BAlert);
  vue.component('BButton', BButton);
  vue.component('BNavbar', BNavbar);
  vue.component('BNavbarNav', BNavbarNav);
  vue.component('BNavbarBrand', BNavbarBrand);
  vue.component('BNavbarToggle', BNavbarToggle);
  vue.component('BPagination', BPagination);
  vue.component('BProgress', BProgress);
  vue.component('BProgressBar', BProgressBar);
  vue.component('BForm', BForm);
  vue.component('BFormInput', BFormInput);
  vue.component('BFormGroup', BFormGroup);
  vue.component('BFormCheckbox', BFormCheckbox);
  vue.component('BCollapse', BCollapse);
  vue.component('BNavItem', BNavItem);
  vue.component('BNavItemDropdown', BNavItemDropdown);
  vue.component('BModal', BModal);
  vue.directive('b-modal', VBModal);
  vue.component('BFormDatepicker', BFormDatepicker);
  vue.component('BInputGroup', BInputGroup);
  vue.component('BInputGroupPrepend', BInputGroupPrepend);
  vue.component('BListGroup', BListGroup);
  vue.component('BListGroupItem', BListGroupItem);
  vue.component('BDropdownText', BDropdownText);
  vue.component('BDropdownDivider', BDropdownDivider);
  vue.component('BSpinner', BSpinner);
  vue.directive('b-tooltip', VBTooltip);
  vue.component('BImg', BImg);
  vue.component('VMultiselect', Multiselect);
  vue.component('BFormTextarea', BFormTextarea);
  vue.component('BFormSelect', BFormSelect);
  vue.component('BFormInvalidFeedback', BFormInvalidFeedback);
}
