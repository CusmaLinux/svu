import axios from 'axios';

const baseApiUrl = 'api/reports';

/**
 * Service class for handling report generation and downloading.
 * This is a general-purpose service that can be extended for different types of reports.
 */
export default class ReportService {
  /**
   * Fetches the PQRS report Excel file as a Blob.
   * A Blob (Binary Large Object) is used to represent the binary data of the file.
   *
   * @param informPqrsId The ID of the InformePqrs entity.
   * @returns A Promise that resolves with the Excel file as a Blob.
   */
  public downloadPqrsReport(informPqrsId: string): Promise<Blob> {
    return new Promise<Blob>((resolve, reject) => {
      axios
        .get(`${baseApiUrl}/pqrs/${informPqrsId}/download`, {
          responseType: 'blob',
        })
        .then(res => {
          resolve(res.data);
        })
        .catch(err => {
          reject(err);
        });
    });
  }
}
