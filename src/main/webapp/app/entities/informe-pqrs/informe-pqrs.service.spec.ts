/* tslint:disable max-line-length */
import axios from 'axios';
import sinon from 'sinon';
import dayjs from 'dayjs';

import InformePqrsService from './informe-pqrs.service';
import { DATE_TIME_FORMAT } from '@/shared/composables/date-format';
import { InformePqrs } from '@/shared/model/informe-pqrs.model';

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
  describe('InformePqrs Service', () => {
    let service: InformePqrsService;
    let elemDefault;
    let currentDate: Date;

    beforeEach(() => {
      service = new InformePqrsService();
      currentDate = new Date();
      elemDefault = new InformePqrs('ABC', currentDate, currentDate, 0, 0, 0);
    });

    describe('Service methods', () => {
      it('should find an element', async () => {
        const returnedFromService = {
          fechaInicio: dayjs(currentDate).format(DATE_TIME_FORMAT),
          fechaFin: dayjs(currentDate).format(DATE_TIME_FORMAT),
          ...elemDefault,
        };
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

      it('should create a InformePqrs', async () => {
        const returnedFromService = {
          id: 'ABC',
          fechaInicio: dayjs(currentDate).format(DATE_TIME_FORMAT),
          fechaFin: dayjs(currentDate).format(DATE_TIME_FORMAT),
          ...elemDefault,
        };
        const expected = { fechaInicio: currentDate, fechaFin: currentDate, ...returnedFromService };

        axiosStub.post.resolves({ data: returnedFromService });
        return service.create({}).then(res => {
          expect(res).toMatchObject(expected);
        });
      });

      it('should not create a InformePqrs', async () => {
        axiosStub.post.rejects(error);

        return service
          .create({})
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });

      it('should update a InformePqrs', async () => {
        const returnedFromService = {
          fechaInicio: dayjs(currentDate).format(DATE_TIME_FORMAT),
          fechaFin: dayjs(currentDate).format(DATE_TIME_FORMAT),
          totalPqrs: 1,
          totalResueltas: 1,
          totalPendientes: 1,
          ...elemDefault,
        };

        const expected = { fechaInicio: currentDate, fechaFin: currentDate, ...returnedFromService };
        axiosStub.put.resolves({ data: returnedFromService });

        return service.update(expected).then(res => {
          expect(res).toMatchObject(expected);
        });
      });

      it('should not update a InformePqrs', async () => {
        axiosStub.put.rejects(error);

        return service
          .update({})
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });

      it('should partial update a InformePqrs', async () => {
        const patchObject = {
          fechaInicio: dayjs(currentDate).format(DATE_TIME_FORMAT),
          totalPqrs: 1,
          totalResueltas: 1,
          totalPendientes: 1,
          ...new InformePqrs(),
        };
        const returnedFromService = Object.assign(patchObject, elemDefault);

        const expected = { fechaInicio: currentDate, fechaFin: currentDate, ...returnedFromService };
        axiosStub.patch.resolves({ data: returnedFromService });

        return service.partialUpdate(patchObject).then(res => {
          expect(res).toMatchObject(expected);
        });
      });

      it('should not partial update a InformePqrs', async () => {
        axiosStub.patch.rejects(error);

        return service
          .partialUpdate({})
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });

      it('should return a list of InformePqrs', async () => {
        const returnedFromService = {
          fechaInicio: dayjs(currentDate).format(DATE_TIME_FORMAT),
          fechaFin: dayjs(currentDate).format(DATE_TIME_FORMAT),
          totalPqrs: 1,
          totalResueltas: 1,
          totalPendientes: 1,
          ...elemDefault,
        };
        const expected = { fechaInicio: currentDate, fechaFin: currentDate, ...returnedFromService };
        axiosStub.get.resolves([returnedFromService]);
        return service.retrieve({ sort: {}, page: 0, size: 10 }).then(res => {
          expect(res).toContainEqual(expected);
        });
      });

      it('should not return a list of InformePqrs', async () => {
        axiosStub.get.rejects(error);

        return service
          .retrieve()
          .then()
          .catch(err => {
            expect(err).toMatchObject(error);
          });
      });

      it('should delete a InformePqrs', async () => {
        axiosStub.delete.resolves({ ok: true });
        return service.delete('ABC').then(res => {
          expect(res.ok).toBeTruthy();
        });
      });

      it('should not delete a InformePqrs', async () => {
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
