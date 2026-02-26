<template>
  <div class="row justify-content-center">
    <div class="col-8">
      <div v-if="oficina">
        <h2 class="jh-entity-heading" data-cy="oficinaDetailsHeading"><span>Ofincina: </span> {{ oficina.nombre }}</h2>
        <dl class="row jh-entity-details">
          <dt>
            <span v-text="t$('ventanillaUnicaApp.oficina.id')"></span>
          </dt>
          <dd>
            <span>{{ oficina.id }}</span>
          </dd>
          <dt>
            <span v-text="t$('ventanillaUnicaApp.oficina.descripcion')"></span>
          </dt>
          <dd>
            <span>{{ oficina.descripcion }}</span>
          </dd>
          <dt>
            <span v-text="t$('ventanillaUnicaApp.oficina.nivel')"></span>
          </dt>
          <dd>
            <span>{{ oficina.nivel }}</span>
          </dd>
          <dt>
            <span v-text="t$('ventanillaUnicaApp.oficina.oficinaSuperior')"></span>
          </dt>
          <dd>
            <span>{{ oficina.oficinaSuperior }}</span>
          </dd>
          <dt>
            <span>Responsable: </span>
          </dt>
          <dd>
            <span>{{ oficina.responsableDTO?.login }}</span>
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span v-text="t$('entity.action.back')"></span>
        </button>
        <router-link v-if="oficina.id" :to="{ name: 'OficinaEdit', params: { oficinaId: oficina.id } }" custom v-slot="scope">
          <button @click="scope?.navigate" class="btn btn-primary">
            <font-awesome-icon icon="pencil-alt"></font-awesome-icon>&nbsp;<span v-text="t$('entity.action.edit')"></span>
          </button>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./oficina-details.component.ts"></script>
