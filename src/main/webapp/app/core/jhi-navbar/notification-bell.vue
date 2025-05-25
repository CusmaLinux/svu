<template>
  <b-nav-item-dropdown class="notification-bell" right data-cy="notificationMenu">
    <template #button-content>
      <b-icon font-scale="1.2" icon="bell-fill"></b-icon>
      <b-badge v-if="unreadCount > 0" variant="danger" class="notification-count">{{ unreadCount }}</b-badge>
    </template>
    <b-dropdown-text v-if="isLoading" class="text-center"> <b-spinner small></b-spinner> Loading... </b-dropdown-text>
    <b-dropdown-item
      v-for="notification in notificationsToDisplay"
      :key="notification.id"
      @click="handleNotificationClick(notification)"
      :data-cy="`notification-${notification.id}`"
      :class="[{ 'notification-bg--read': notification.read, 'notification-bg': !notification.read }]"
    >
      <div class="text-white">
        <small>{{ t$(getEnumValueByKey(NotificationType, notification.type, t$('sse-notification.non-type'))) }}</small
        ><br />
        {{ truncateMessage(notification || '', 300) }}
      </div>
      <small>{{ formatDateTime(notification.creationDate) }}</small>
    </b-dropdown-item>
    <b-dropdown-divider v-if="notificationsToDisplay.length > 0"></b-dropdown-divider>
    <b-dropdown-item v-if="notificationsToDisplay.length === 0 && !isLoading" data-cy="noNotifications">
      {{ t$('sse-notification.no-new-notifications') }}
    </b-dropdown-item>
    <b-dropdown-item v-if="unreadCount > 0" @click="markAllAsRead" data-cy="markAllRead">
      {{ t$('sse-notification.mark-all-as-read') }}</b-dropdown-item
    >
    <b-dropdown-item :to="{ name: 'Notifications' }" v-if="isNotificationEntityGenerated" data-cy="viewAllNotifications">
      {{ t$('sse-notification.view-all-notifications') }}
    </b-dropdown-item>
  </b-nav-item-dropdown>
</template>

<script lang="ts" src="./notification-bell.component.js"></script>

<style scoped lang="scss">
@mixin notification-item-styles($base-bg-color, $hover-bg-color, $active-bg-color, $text-color: white) {
  background-color: $base-bg-color;
  transition: background-color 0.15s ease-in-out;

  a.dropdown-item {
    background-color: transparent;
    color: $text-color;
    transition:
      background-color 0.15s ease-in-out,
      color 0.15s ease-in-out;

    > * {
      color: $text-color;
    }
    .b-icon {
      color: $text-color;
    }
  }

  &:hover,
  &:focus-within {
    background-color: $hover-bg-color;

    a.dropdown-item {
      background-color: transparent;
      color: $text-color;

      > * {
        color: $text-color;
      }
      .b-icon {
        color: $text-color;
      }
    }
  }

  &:active,
  &.active {
    background-color: $active-bg-color;
    a.dropdown-item {
      background-color: transparent;
      color: $text-color;

      > * {
        color: $text-color;
      }
      .b-icon {
        color: $text-color;
      }
    }
  }
}

.notification-bell {
  .notification-count {
    position: absolute;
    top: 8px;
    right: 8px;
    font-size: 0.65rem;
    padding: 0.15em 0.35em;
  }

  :deep(li.notification-bg) {
    @include notification-item-styles($base-bg-color: #6baed8, $hover-bg-color: #559cd1, $active-bg-color: #428bca);
  }
  :deep(li.notification-bg--read) {
    @include notification-item-styles($base-bg-color: #28a745, $hover-bg-color: #b7e2c1, $active-bg-color: #428bca);
  }
}
</style>
