import axios from 'axios';

import { type IOficina } from '@/shared/model/oficina.model';
import buildPaginationQueryOpts from '@/shared/sort/sorts';

const baseApiUrl = 'api/oficinas';

export default class OficinaService {
  public find(id: string | string[]): Promise<IOficina> {
    return new Promise<IOficina>((resolve, reject) => {
      axios
        .get(`${baseApiUrl}/${id}`)
        .then(res => {
          resolve(res.data);
        })
        .catch(err => {
          reject(err);
        });
    });
  }

  public retrieve(paginationQuery: any): Promise<any> {
    const queryOpts = buildPaginationQueryOpts(paginationQuery);
    return new Promise<any>((resolve, reject) => {
      axios
        .get(`${baseApiUrl}?${queryOpts}`)
        .then(res => {
          resolve(res);
        })
        .catch(err => {
          reject(err);
        });
    });
  }

  public delete(id: string | null | undefined): Promise<any> {
    return new Promise<any>((resolve, reject) => {
      axios
        .delete(`${baseApiUrl}/${id}`)
        .then(res => {
          resolve(res);
        })
        .catch(err => {
          reject(err);
        });
    });
  }

  public create(entity: IOficina): Promise<IOficina> {
    return new Promise<IOficina>((resolve, reject) => {
      axios
        .post(`${baseApiUrl}`, entity)
        .then(res => {
          resolve(res.data);
        })
        .catch(err => {
          reject(err);
        });
    });
  }

  public update(entity: IOficina): Promise<IOficina> {
    return new Promise<IOficina>((resolve, reject) => {
      axios
        .put(`${baseApiUrl}/${entity.id}`, entity)
        .then(res => {
          resolve(res.data);
        })
        .catch(err => {
          reject(err);
        });
    });
  }

  public partialUpdate(entity: IOficina): Promise<IOficina> {
    return new Promise<IOficina>((resolve, reject) => {
      axios
        .patch(`${baseApiUrl}/${entity.id}`, entity)
        .then(res => {
          resolve(res.data);
        })
        .catch(err => {
          reject(err);
        });
    });
  }

  public search(paginationQuery: any, query?: string): Promise<any> {
    let queryOpts = buildPaginationQueryOpts(paginationQuery);

    if (query && query.trim() !== '') {
      queryOpts += `&query=${encodeURIComponent(query)}`;
    }

    return axios.get(`${baseApiUrl}/search?${queryOpts}`);
  }
}
