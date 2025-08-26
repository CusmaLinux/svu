import axios from 'axios';
import buildPaginationQueryOpts from '@/shared/sort/sorts';

const baseApiUrl = 'api/admin/non-business-days';

export default class SpecialDatesService {
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

  public create(data: any): Promise<any> {
    return new Promise<any>((resolve, reject) => {
      axios
        .post(baseApiUrl, data)
        .then(res => {
          resolve(res.data);
        })
        .catch(err => {
          reject(err);
        });
    });
  }

  public update(data: any): Promise<any> {
    return new Promise<any>((resolve, reject) => {
      axios
        .put(`${baseApiUrl}/${data.id}`, data)
        .then(res => {
          resolve(res.data);
        })
        .catch(err => {
          reject(err);
        });
    });
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
}
