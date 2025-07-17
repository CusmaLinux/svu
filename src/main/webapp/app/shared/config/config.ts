import { type IntlDateTimeFormats, createI18n } from 'vue-i18n';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';

import { library } from '@fortawesome/fontawesome-svg-core';
import { faArrowLeft } from '@fortawesome/free-solid-svg-icons/faArrowLeft';
import { faAsterisk } from '@fortawesome/free-solid-svg-icons/faAsterisk';
import { faBan } from '@fortawesome/free-solid-svg-icons/faBan';
import { faBars } from '@fortawesome/free-solid-svg-icons/faBars';
import { faBell } from '@fortawesome/free-solid-svg-icons/faBell';
import { faBook } from '@fortawesome/free-solid-svg-icons/faBook';
import { faCloud } from '@fortawesome/free-solid-svg-icons/faCloud';
import { faCogs } from '@fortawesome/free-solid-svg-icons/faCogs';
import { faDatabase } from '@fortawesome/free-solid-svg-icons/faDatabase';
import { faEye } from '@fortawesome/free-solid-svg-icons/faEye';
import { faFlag } from '@fortawesome/free-solid-svg-icons/faFlag';
import { faHeart } from '@fortawesome/free-solid-svg-icons/faHeart';
import { faHome } from '@fortawesome/free-solid-svg-icons/faHome';
import { faList } from '@fortawesome/free-solid-svg-icons/faList';
import { faLock } from '@fortawesome/free-solid-svg-icons/faLock';
import { faPencilAlt } from '@fortawesome/free-solid-svg-icons/faPencilAlt';
import { faPlus } from '@fortawesome/free-solid-svg-icons/faPlus';
import { faRoad } from '@fortawesome/free-solid-svg-icons/faRoad';
import { faSave } from '@fortawesome/free-solid-svg-icons/faSave';
import { faSearch } from '@fortawesome/free-solid-svg-icons/faSearch';
import { faSignInAlt } from '@fortawesome/free-solid-svg-icons/faSignInAlt';
import { faSignOutAlt } from '@fortawesome/free-solid-svg-icons/faSignOutAlt';
import { faSort } from '@fortawesome/free-solid-svg-icons/faSort';
import { faSortDown } from '@fortawesome/free-solid-svg-icons/faSortDown';
import { faSortUp } from '@fortawesome/free-solid-svg-icons/faSortUp';
import { faSync } from '@fortawesome/free-solid-svg-icons/faSync';
import { faTachometerAlt } from '@fortawesome/free-solid-svg-icons/faTachometerAlt';
import { faTasks } from '@fortawesome/free-solid-svg-icons/faTasks';
import { faThList } from '@fortawesome/free-solid-svg-icons/faThList';
import { faTimesCircle } from '@fortawesome/free-solid-svg-icons/faTimesCircle';
import { faTimes } from '@fortawesome/free-solid-svg-icons/faTimes';
import { faTrash } from '@fortawesome/free-solid-svg-icons/faTrash';
import { faUser } from '@fortawesome/free-solid-svg-icons/faUser';
import { faUserPlus } from '@fortawesome/free-solid-svg-icons/faUserPlus';
import { faUsers } from '@fortawesome/free-solid-svg-icons/faUsers';
import { faUsersCog } from '@fortawesome/free-solid-svg-icons/faUsersCog';
import { faWrench } from '@fortawesome/free-solid-svg-icons/faWrench';
import { faCircleInfo } from '@fortawesome/free-solid-svg-icons/faCircleInfo';
import { faTriangleExclamation } from '@fortawesome/free-solid-svg-icons/faTriangleExclamation';
import { faCopy } from '@fortawesome/free-solid-svg-icons/faCopy';
import { faHeadset } from '@fortawesome/free-solid-svg-icons/faHeadset';
import { faPaperPlane } from '@fortawesome/free-solid-svg-icons/faPaperPlane';
import { faPaperclip } from '@fortawesome/free-solid-svg-icons/faPaperclip';
import { faFileLines } from '@fortawesome/free-solid-svg-icons/faFileLines';
import { faPlusCircle } from '@fortawesome/free-solid-svg-icons/faPlusCircle';
import { faIdCard } from '@fortawesome/free-solid-svg-icons/faIdCard';
import { faFolderOpen } from '@fortawesome/free-solid-svg-icons/faFolderOpen';
import { faComments } from '@fortawesome/free-solid-svg-icons/faComments';
import { faComment } from '@fortawesome/free-solid-svg-icons/faComment';
import { faTrashCan } from '@fortawesome/free-solid-svg-icons/faTrashCan';
import { faGear } from '@fortawesome/free-solid-svg-icons/faGear';
import { faRotateLeft } from '@fortawesome/free-solid-svg-icons/faRotateLeft';
import { faCheckCircle } from '@fortawesome/free-solid-svg-icons/faCheckCircle';
import { faDownload } from '@fortawesome/free-solid-svg-icons/faDownload';
import { faPenToSquare } from '@fortawesome/free-solid-svg-icons/faPenToSquare';
import { faLocationDot } from '@fortawesome/free-solid-svg-icons/faLocationDot';
import { faEnvelope } from '@fortawesome/free-solid-svg-icons/faEnvelope';
import { faPhone } from '@fortawesome/free-solid-svg-icons/faPhone';
import { faUpload } from '@fortawesome/free-solid-svg-icons/faUpload';
import { faAt } from '@fortawesome/free-solid-svg-icons/faAt';
import { faListUl } from '@fortawesome/free-solid-svg-icons/faListUl';
import { faTag } from '@fortawesome/free-solid-svg-icons/faTag';
import { faHashtag } from '@fortawesome/free-solid-svg-icons/faHashtag';
import { faSitemap } from '@fortawesome/free-solid-svg-icons/faSitemap';
import { faHourglassHalf } from '@fortawesome/free-solid-svg-icons/faHourglassHalf';
import { faCalendarDay } from '@fortawesome/free-solid-svg-icons/faCalendarDay';
import { faCalendarCheck } from '@fortawesome/free-solid-svg-icons/faCalendarCheck';
import { faCloudArrowUp } from '@fortawesome/free-solid-svg-icons/faCloudArrowUp';
import { faFilter } from '@fortawesome/free-solid-svg-icons/faFilter';
import { faAlignLeft } from '@fortawesome/free-solid-svg-icons/faAlignLeft';
import { faBuilding } from '@fortawesome/free-solid-svg-icons/faBuilding';
import { faLayerGroup } from '@fortawesome/free-solid-svg-icons/faLayerGroup';
import { faUserGear } from '@fortawesome/free-solid-svg-icons/faUserGear';
import { faLanguage } from '@fortawesome/free-solid-svg-icons/faLanguage';
import { faCircle } from '@fortawesome/free-solid-svg-icons/faCircle';
import { faCircleNotch } from '@fortawesome/free-solid-svg-icons/faCircleNotch';
import { faCheckDouble } from '@fortawesome/free-solid-svg-icons/faCheckDouble';

const datetimeFormats: IntlDateTimeFormats = {
  es: {
    short: {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: 'numeric',
    },
    medium: {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      weekday: 'short',
      hour: 'numeric',
      minute: 'numeric',
    },
    long: {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      weekday: 'long',
      hour: 'numeric',
      minute: 'numeric',
    },
  },
  en: {
    short: {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: 'numeric',
    },
    medium: {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      weekday: 'short',
      hour: 'numeric',
      minute: 'numeric',
    },
    long: {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      weekday: 'long',
      hour: 'numeric',
      minute: 'numeric',
    },
  },
  // jhipster-needle-i18n-language-date-time-format - JHipster will add/remove format options in this object
};

export function initFortAwesome(vue) {
  vue.component('font-awesome-icon', FontAwesomeIcon);

  library.add(
    faArrowLeft,
    faAsterisk,
    faBan,
    faBars,
    faBell,
    faBook,
    faCloud,
    faCogs,
    faDatabase,
    faEye,
    faFlag,
    faHeart,
    faHome,
    faList,
    faLock,
    faPencilAlt,
    faPlus,
    faRoad,
    faSave,
    faSearch,
    faSignInAlt,
    faSignOutAlt,
    faSort,
    faSortDown,
    faSortUp,
    faSync,
    faTachometerAlt,
    faTasks,
    faThList,
    faTimes,
    faTimesCircle,
    faTrash,
    faUser,
    faUserPlus,
    faUsers,
    faUsersCog,
    faWrench,
    faCircleInfo,
    faTriangleExclamation,
    faCopy,
    faHeadset,
    faPaperPlane,
    faPaperclip,
    faFileLines,
    faPlusCircle,
    faIdCard,
    faFolderOpen,
    faComments,
    faComment,
    faTrashCan,
    faGear,
    faRotateLeft,
    faCheckCircle,
    faDownload,
    faPenToSquare,
    faLocationDot,
    faEnvelope,
    faPhone,
    faUpload,
    faAt,
    faListUl,
    faTag,
    faHashtag,
    faSitemap,
    faHourglassHalf,
    faCalendarDay,
    faCalendarCheck,
    faCloudArrowUp,
    faFilter,
    faAlignLeft,
    faBuilding,
    faLayerGroup,
    faUserGear,
    faLanguage,
    faCircle,
    faCircleNotch,
    faCheckDouble,
  );
}
export function initI18N(opts: any = {}) {
  return createI18n({
    missingWarn: false,
    fallbackWarn: false,
    legacy: false,
    datetimeFormats,
    silentTranslationWarn: true,
    ...opts,
  });
}
