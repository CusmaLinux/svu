import axios from 'axios';
import buildPaginationQueryOpts from '@/shared/sort/sorts';

const baseApiUrl = 'api/admin';

export default class UserService {
  public retrieve(paginationQuery: any): Promise<any> {
    const queryOpts = buildPaginationQueryOpts(paginationQuery);
    return new Promise<any>((resolve, reject) => {
      axios
        .get(`${baseApiUrl}/users?${queryOpts}`)
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

    return axios.get(`${baseApiUrl}/users/search?${queryOpts}`);
  }
}
