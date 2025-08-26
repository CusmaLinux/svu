<template>
  <b-container>
    <h2 id="special-dates-heading" data-cy="SpecialDatesHeading">
      <span>Calendario de Días Festivos y Eventos</span>
      <div class="d-flex justify-content-end">
        <b-button class="mr-2" @click="handleSyncList" :disabled="isFetching" variant="info">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon>
          <span>Refrescar Lista</span>
        </b-button>
      </div>
    </h2>

    <div ref="formCard">
      <b-card no-body class="shadow-sm mb-4 border-0">
        <b-card-header class="bg-light py-3">
          <h3 class="mb-0 h4 font-weight-bold">{{ isUpdating ? 'Editar' : 'Añadir' }} Día Festivo o Evento</h3>
        </b-card-header>
        <b-card-body>
          <b-form @submit.prevent="saveSpecialDate">
            <b-row>
              <b-col md="5">
                <b-form-group label="Fecha:" label-for="special-date-date" label-class="font-weight-bold">
                  <b-input-group>
                    <b-input-group-prepend is-text>
                      <font-awesome-icon icon="calendar-day" />
                    </b-input-group-prepend>
                    <b-form-datepicker
                      id="special-date-date"
                      v-model="specialDateForm.date"
                      required
                      placeholder="Seleccione una fecha"
                      :date-format-options="{ year: 'numeric', month: 'numeric', day: 'numeric' }"
                      data-cy="date"
                    ></b-form-datepicker>
                  </b-input-group>
                </b-form-group>
              </b-col>
              <b-col md="7">
                <b-form-group label="Descripción:" label-for="special-date-description" label-class="font-weight-bold">
                  <b-input-group>
                    <b-input-group-prepend is-text>
                      <font-awesome-icon icon="tag" />
                    </b-input-group-prepend>
                    <b-form-input
                      id="special-date-description"
                      v-model="specialDateForm.description"
                      type="text"
                      required
                      placeholder="Ej: Semana Universitaria"
                      data-cy="description"
                    ></b-form-input>
                  </b-input-group>
                </b-form-group>
              </b-col>
            </b-row>
            <div class="d-flex justify-content-end mt-3 border-top pt-3">
              <b-button v-if="isUpdating" variant="secondary" class="mr-2" @click="resetForm()">
                <font-awesome-icon icon="ban"></font-awesome-icon>
                <span>Cancelar</span>
              </b-button>
              <b-button
                type="submit"
                variant="primary"
                :disabled="!specialDateForm.date || !specialDateForm.description"
                data-cy="entitySaveButton"
              >
                <font-awesome-icon icon="save"></font-awesome-icon>
                <span>{{ isUpdating ? 'Actualizar' : 'Guardar' }}</span>
              </b-button>
            </div>
          </b-form>
        </b-card-body>
      </b-card>
    </div>

    <div class="alert alert-warning" v-if="!isFetching && (!specialDates || specialDates.length === 0)">
      <span>No se han configurado días no hábiles.</span>
    </div>

    <div v-if="specialDates && specialDates.length > 0">
      <b-table striped hover :items="specialDates" :fields="tableFields" responsive="sm">
        <template #head(date)="data">
          <div @click="changeOrder('date')" style="cursor: pointer; display: inline-block">
            {{ data.label }}
            <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'date'"></jhi-sort-indicator>
          </div>
        </template>

        <template #cell(date)="data">
          {{ new Date(data.value).toLocaleDateString('es-CO', { timeZone: 'UTC', year: 'numeric', month: 'long', day: 'numeric' }) }}
        </template>

        <template #cell(actions)="row">
          <div class="btn-group">
            <b-button @click="handleUpdate(row.item)" variant="primary" size="sm" class="edit" data-cy="entityEditButton">
              <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
              <span class="d-none d-md-inline">Editar</span>
            </b-button>
            <b-button @click="prepareRemove(row.item)" variant="danger" size="sm" class="delete" data-cy="entityDeleteButton">
              <font-awesome-icon icon="times"></font-awesome-icon>
              <span class="d-none d-md-inline">Eliminar</span>
            </b-button>
          </div>
        </template>
      </b-table>
    </div>

    <div v-show="specialDates && specialDates.length > 0">
      <div class="row justify-content-center">
        <jhi-item-count :page="page" :total="queryCount" :itemsPerPage="itemsPerPage"></jhi-item-count>
      </div>
      <div class="row justify-content-center">
        <b-pagination size="md" :total-rows="totalItems" v-model="page" :per-page="itemsPerPage"></b-pagination>
      </div>
    </div>

    <b-modal ref="removeEntity" id="removeEntity" title="Confirmar Eliminación">
      <div class="modal-body">
        <p>¿Está seguro de que desea eliminar esta fecha?</p>
        <p v-if="specialDateToRemove">
          <strong>{{ specialDateToRemove.description }} ({{ new Date(specialDateToRemove.date).toLocaleDateString('es-CO') }})</strong>
        </p>
      </div>
      <template #modal-footer>
        <button type="button" class="btn btn-secondary" @click="closeDeleteDialog">Cancelar</button>
        <button type="button" class="btn btn-danger" @click="removeSpecialDate">Eliminar</button>
      </template>
    </b-modal>
  </b-container>
</template>

<script lang="ts" src="./special-dates.component.ts"></script>
