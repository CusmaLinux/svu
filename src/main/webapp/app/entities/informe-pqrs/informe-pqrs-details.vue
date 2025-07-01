<template>
  <div class="row justify-content-center">
    <div class="col-8">
      <div v-if="informePqrs">
        <h2 class="jh-entity-heading" data-cy="informePqrsDetailsHeading">
          <span v-text="t$('ventanillaUnicaApp.informePqrs.detail.title')"></span> {{ informePqrs.id }}
        </h2>
        <dl class="row jh-entity-details">
          <dt>
            <span v-text="t$('ventanillaUnicaApp.informePqrs.fechaInicio')"></span>
          </dt>
          <dd>
            <span v-if="informePqrs.fechaInicio">{{ formatDateLong(informePqrs.fechaInicio) }}</span>
          </dd>
          <dt>
            <span v-text="t$('ventanillaUnicaApp.informePqrs.fechaFin')"></span>
          </dt>
          <dd>
            <span v-if="informePqrs.fechaFin">{{ formatDateLong(informePqrs.fechaFin) }}</span>
          </dd>
          <dt>
            <span v-text="t$('ventanillaUnicaApp.informePqrs.totalPqrs')"></span>
          </dt>
          <dd>
            <span>{{ informePqrs.totalPqrs }}</span>
          </dd>
          <dt>
            <span v-text="t$('ventanillaUnicaApp.informePqrs.totalResueltas')"></span>
          </dt>
          <dd>
            <span>{{ informePqrs.totalResueltas }}</span>
          </dd>
          <dt>
            <span v-text="t$('ventanillaUnicaApp.informePqrs.totalPendientes')"></span>
          </dt>
          <dd>
            <span>{{ informePqrs.totalPendientes }}</span>
          </dd>
          <dt>
            <span v-text="t$('ventanillaUnicaApp.informePqrs.oficina')"></span>
          </dt>
          <dd>
            <div v-if="informePqrs.oficina">
              <router-link :to="{ name: 'OficinaView', params: { oficinaId: informePqrs.oficina.id } }">{{
                informePqrs.oficina.nombre
              }}</router-link>
            </div>
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span v-text="t$('entity.action.back')"></span>
        </button>
        <router-link
          v-if="informePqrs.id"
          :to="{ name: 'InformePqrsEdit', params: { informePqrsId: informePqrs.id } }"
          custom
          v-slot="{ navigate }"
        >
          <button @click="navigate" class="btn btn-primary">
            <font-awesome-icon icon="pencil-alt"></font-awesome-icon>&nbsp;<span v-text="t$('entity.action.edit')"></span>
          </button>
          <button type="button" class="btn btn-info" @click="downloadReport(informePqrs.id)" :disabled="isDownloading">
            <font-awesome-icon icon="download"></font-awesome-icon>
            <span v-if="!isDownloading">Descargar Informe (.xlsx)</span>
            <span v-else>Descargando...</span>
          </button>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./informe-pqrs-details.component.ts"></script>
