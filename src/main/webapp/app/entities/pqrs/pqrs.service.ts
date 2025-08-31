import axios from 'axios';

import buildPaginationQueryOpts from '@/shared/sort/sorts';

import { type IPqrs } from '@/shared/model/pqrs.model';

const baseApiUrl = 'api/pqrs';

const publicApiUrl = 'api/public/pqrs';

export default class PqrsService {
  public find(id: string | string[]): Promise<IPqrs> {
    return new Promise<IPqrs>((resolve, reject) => {
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

  public retrieveAccessTokenByFileNumber(fileNumber: string, headers?: any): Promise<string> {
    return new Promise<string>((resolve, reject) => {
      axios
        .get(`${publicApiUrl}/consult/${fileNumber}`, { headers })
        .then(res => {
          resolve(res.data);
        })
        .catch(err => {
          reject(err);
        });
    });
  }

  public findPublicByAccessToken(accessToken: string): Promise<IPqrs> {
    return new Promise<IPqrs>((resolve, reject) => {
      axios
        .get(`${publicApiUrl}/${accessToken}`)
        .then(res => {
          resolve(res.data);
        })
        .catch(err => {
          reject(err);
        });
    });
  }

  public submitPublicResponse(accessToken: string, formData: FormData, headers?: any): Promise<IPqrs> {
    return new Promise<IPqrs>((resolve, reject) => {
      axios
        .post(`${publicApiUrl}/${accessToken}/responses`, formData, { headers })
        .then(res => {
          resolve(res.data);
        })
        .catch(err => {
          reject(err);
        });
    });
  }

  public retrieve(paginationQuery?: any): Promise<any> {
    return new Promise<any>((resolve, reject) => {
      axios
        .get(`${baseApiUrl}?${buildPaginationQueryOpts(paginationQuery)}`)
        .then(res => {
          resolve(res);
        })
        .catch(err => {
          reject(err);
        });
    });
  }

  public askOffice(id: string): Promise<any> {
    return new Promise<any>((resolve, reject) => {
      axios
        .get(`${baseApiUrl}/${id}/suggest-office`)
        .then(res => {
          resolve(res);
        })
        .catch(err => {
          reject(err);
        });
    });
  }

  public assignOffice(id: string, officeName: string): Promise<any> {
    return new Promise<any>((resolve, reject) => {
      axios
        .put(`${baseApiUrl}/${id}/office`, null, { params: { officeName: officeName } })
        .then(res => {
          resolve(res);
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

  public delete(id: string): Promise<any> {
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

  public create(entity: IPqrs): Promise<IPqrs> {
    return new Promise<IPqrs>((resolve, reject) => {
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

  public submitPqrsRequest(entity: IPqrs, headers?: any): Promise<IPqrs> {
    return new Promise<IPqrs>((resolve, reject) => {
      axios
        .post(`${publicApiUrl}`, entity, { headers })
        .then(res => {
          resolve(res.data);
        })
        .catch(err => {
          reject(err);
        });
    });
  }

  public update(entity: IPqrs): Promise<IPqrs> {
    return new Promise<IPqrs>((resolve, reject) => {
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

  public partialUpdate(entity: IPqrs): Promise<IPqrs> {
    return new Promise<IPqrs>((resolve, reject) => {
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
}
