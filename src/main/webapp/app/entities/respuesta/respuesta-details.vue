<template>
  <div class="row justify-content-center">
    <div class="col-8">
      <div v-if="respuesta">
        <h2 class="jh-entity-heading" data-cy="respuestaDetailsHeading">
          <span v-text="t$('ventanillaUnicaApp.respuesta.detail.title')"></span> {{ respuesta.id }}
        </h2>
        <dl class="row jh-entity-details">
          <dt>
            <span v-text="t$('ventanillaUnicaApp.respuesta.contenido')"></span>
          </dt>
          <dd>
            <span>{{ respuesta.contenido }}</span>
          </dd>
          <dt>
            <span v-text="t$('ventanillaUnicaApp.respuesta.fechaRespuesta')"></span>
          </dt>
          <dd>
            <span v-if="respuesta.fechaRespuesta">{{ formatDateLong(respuesta.fechaRespuesta) }}</span>
          </dd>
          <dt>
            <span v-text="t$('ventanillaUnicaApp.respuesta.pqr')"></span>
          </dt>
          <dd>
            <div v-if="respuesta.pqrs">
              <router-link :to="{ name: 'PqrsView', params: { pqrsId: respuesta.pqrs.id } }">{{ respuesta.pqrs.id }}</router-link>
            </div>
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span v-text="t$('entity.action.back')"></span>
        </button>
        <router-link
          v-if="respuesta.id"
          :to="{ name: 'RespuestaEdit', params: { respuestaId: respuesta.id } }"
          custom
          v-slot="{ navigate }"
        >
          <button v-can="['edit', 'responses']" @click="navigate" class="btn btn-primary">
            <font-awesome-icon icon="pencil-alt"></font-awesome-icon>&nbsp;<span v-text="t$('entity.action.edit')"></span>
          </button>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./respuesta-details.component.ts"></script>
