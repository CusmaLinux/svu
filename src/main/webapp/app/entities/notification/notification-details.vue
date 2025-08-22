<template>
  <div class="row justify-content-center">
    <div class="col-8">
      <div v-if="notification">
        <h2 class="jh-entity-heading" data-cy="notificationDetailsHeading">
          <span v-text="t$('ventanillaUnicaApp.notification.detail.title')"></span>
        </h2>
        <dl class="row jh-entity-details">
          <dt>
            <span v-text="t$('ventanillaUnicaApp.notification.id')"></span>
          </dt>
          <dd>
            <span>{{ notification.id }}</span>
          </dd>
          <dt>
            <span v-text="t$('ventanillaUnicaApp.notification.tipo')"></span>
          </dt>
          <dd>
            <span>{{ t$(getEnumValueByKey(NotificationType, notification.tipo, t$('sse-notification.non-type'))) }}</span>
          </dd>
          <dt>
            <span v-text="t$('ventanillaUnicaApp.notification.fecha')"></span>
          </dt>
          <dd>
            <span v-if="notification.fecha">{{ formatDateLong(notification.fecha) }}</span>
          </dd>
          <dt>
            <span v-text="t$('ventanillaUnicaApp.notification.mensaje')"></span>
          </dt>
          <dd>
            <span>{{ notification.mensaje }}</span>
          </dd>
          <dt>
            <span v-text="t$('ventanillaUnicaApp.notification.leido')"></span>
          </dt>
          <dd>
            <span v-if="notification.leido">
              <font-awesome-icon icon="check-circle" class="text-success" />
            </span>
            <span v-else>
              {{ notification.leido }}
            </span>
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span v-text="t$('entity.action.back')"></span>
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./notification-details.component.ts"></script>
