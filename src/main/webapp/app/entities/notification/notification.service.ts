import axios from 'axios';

import buildPaginationQueryOpts from '@/shared/sort/sorts';

import { type INotification } from '@/shared/model/notification.model';

const baseApiUrl = 'api/notification';

export default class NotificationService {
  public find(id: string | string[]): Promise<INotification> {
    return new Promise<INotification>((resolve, reject) => {
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

  public create(entity: INotification): Promise<INotification> {
    return new Promise<INotification>((resolve, reject) => {
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

  public update(entity: INotification): Promise<INotification> {
    return new Promise<INotification>((resolve, reject) => {
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

  public partialUpdate(entity: INotification): Promise<INotification> {
    return new Promise<INotification>((resolve, reject) => {
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

  public markAsRead(notificationId: string | number | undefined): Promise<any> {
    return new Promise<any>((resolve, reject) => {
      axios
        .put(`${baseApiUrl}/${notificationId}/mark-as-read`)
        .then(res => {
          resolve(res);
        })
        .catch(err => {
          reject(err);
        });
    });
  }

  public markAllAsRead(): Promise<any> {
    return new Promise<any>((resolve, reject) => {
      axios
        .put(`${baseApiUrl}/mark-all-as-read`)
        .then(res => {
          resolve(res);
        })
        .catch(err => {
          reject(err);
        });
    });
  }
}
