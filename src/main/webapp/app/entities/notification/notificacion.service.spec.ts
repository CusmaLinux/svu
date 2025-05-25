/* tslint:disable max-line-length */
import axios from 'axios';
import sinon from 'sinon';
import dayjs from 'dayjs';

import NotificacionService from './notificacion.service';
import { DATE_TIME_FORMAT } from '@/shared/composables/date-format';
import { Notificacion } from '@/shared/model/notificacion.model';

const error = {
  response: {
    status: null,
    data: {
      type: null,
    },
  },
};

const axiosStub = {
  get: sinon.stub(axios, 'get'),
  post: sinon.stub(axios, 'post'),
  put: sinon.stub(axios, 'put'),
  patch: sinon.stub(axios, 'patch'),
  delete: sinon.stub(axios, 'delete'),
};

describe('Service Tests', () => {
  describe('Notificacion Service', () => {
    let service: NotificacionService;
    let elemDefault;
    let currentDate: Date;

    beforeEach(() => {
      service = new NotificacionService();
      currentDate = new Date();
      elemDefault = new Notificacion('ABC', 'AAAAAAA', currentDate, 'AAAAAAA', false);
    });

    describe('Service methods', () => {
      it('should find an element', async () => {
        const returnedFromService = { fecha: dayjs(currentDate).format(DATE_TIME_FORMAT), ...elemDefault };
        axiosStub.get.resolves({ data: returnedFromService });

        return service.find('ABC').then(res => {
          expect(res).toMatchObject(elemDefault);
        });
      });

      it('should not find an element', async () => {
        axiosStub.get.rejects(error);
        return service
          .find('ABC')
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });

      it('should create a Notificacion', async () => {
        const returnedFromService = { id: 'ABC', fecha: dayjs(currentDate).format(DATE_TIME_FORMAT), ...elemDefault };
        const expected = { fecha: currentDate, ...returnedFromService };

        axiosStub.post.resolves({ data: returnedFromService });
        return service.create({}).then(res => {
          expect(res).toMatchObject(expected);
        });
      });

      it('should not create a Notificacion', async () => {
        axiosStub.post.rejects(error);

        return service
          .create({})
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });

      it('should update a Notificacion', async () => {
        const returnedFromService = {
          tipo: 'BBBBBB',
          fecha: dayjs(currentDate).format(DATE_TIME_FORMAT),
          mensaje: 'BBBBBB',
          leido: true,
          ...elemDefault,
        };

        const expected = { fecha: currentDate, ...returnedFromService };
        axiosStub.put.resolves({ data: returnedFromService });

        return service.update(expected).then(res => {
          expect(res).toMatchObject(expected);
        });
      });

      it('should not update a Notificacion', async () => {
        axiosStub.put.rejects(error);

        return service
          .update({})
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });

      it('should partial update a Notificacion', async () => {
        const patchObject = { fecha: dayjs(currentDate).format(DATE_TIME_FORMAT), ...new Notificacion() };
        const returnedFromService = Object.assign(patchObject, elemDefault);

        const expected = { fecha: currentDate, ...returnedFromService };
        axiosStub.patch.resolves({ data: returnedFromService });

        return service.partialUpdate(patchObject).then(res => {
          expect(res).toMatchObject(expected);
        });
      });

      it('should not partial update a Notificacion', async () => {
        axiosStub.patch.rejects(error);

        return service
          .partialUpdate({})
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });

      it('should return a list of Notificacion', async () => {
        const returnedFromService = {
          tipo: 'BBBBBB',
          fecha: dayjs(currentDate).format(DATE_TIME_FORMAT),
          mensaje: 'BBBBBB',
          leido: true,
          ...elemDefault,
        };
        const expected = { fecha: currentDate, ...returnedFromService };
        axiosStub.get.resolves([returnedFromService]);
        return service.retrieve({ sort: {}, page: 0, size: 10 }).then(res => {
          expect(res).toContainEqual(expected);
        });
      });

      it('should not return a list of Notificacion', async () => {
        axiosStub.get.rejects(error);

        return service
          .retrieve()
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });

      it('should delete a Notificacion', async () => {
        axiosStub.delete.resolves({ ok: true });
        return service.delete('ABC').then(res => {
          expect(res.ok).toBeTruthy();
        });
      });

      it('should not delete a Notificacion', async () => {
        axiosStub.delete.rejects(error);

        return service
          .delete('ABC')
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });
    });
  });
});
