<template>
  <div>
    <b-dropdown-item to="/pqrs">
      <font-awesome-icon icon="asterisk" />
      <span v-text="t$('global.menu.entities.pqrs')"></span>
    </b-dropdown-item>
    <b-dropdown-item v-can="['view', 'responses']" to="/respuesta">
      <font-awesome-icon icon="asterisk" />
      <span v-text="t$('global.menu.entities.respuesta')"></span>
    </b-dropdown-item>
    <b-dropdown-item v-can="['view', 'offices']" to="/oficina">
      <font-awesome-icon icon="asterisk" />
      <span v-text="t$('global.menu.entities.oficina')"></span>
    </b-dropdown-item>
    <b-dropdown-item v-can="['view', 'attached_files']" to="/archivo-adjunto">
      <font-awesome-icon icon="asterisk" />
      <span v-text="t$('global.menu.entities.archivoAdjunto')"></span>
    </b-dropdown-item>
    <b-dropdown-item v-can="['view', 'reports']" to="/informe-pqrs">
      <font-awesome-icon icon="asterisk" />
      <span v-text="t$('global.menu.entities.informePqrs')"></span>
    </b-dropdown-item>
    <b-dropdown-item to="/notificaciones">
      <font-awesome-icon icon="asterisk" />
      <span v-text="t$('global.menu.entities.notifications')"></span>
    </b-dropdown-item>
    <!-- jhipster-needle-add-entity-to-menu - JHipster will add entities to the menu here -->
  </div>
</template>

<script lang="ts" src="./entities-menu.component.ts"></script>
