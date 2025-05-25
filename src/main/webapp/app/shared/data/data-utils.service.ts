/**
 * An composable utility for data.
 */
const useDataUtils = () => ({
  /**
   * Method to abbreviate the text given
   */
  abbreviate(text, append = '...') {
    if (text.length < 30) {
      return text;
    }
    return text ? text.substring(0, 15) + append + text.slice(-10) : '';
  },

  /**
   * Method to find the byte size of the string provides
   */
  byteSize(base64String) {
    return this.formatAsBytes(this.size(base64String));
  },

  /**
   * Method to open file
   */
  openFile(contentType, data) {
    const byteCharacters = atob(data);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
      byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    const blob = new Blob([byteArray], {
      type: contentType,
    });
    const objectURL = URL.createObjectURL(blob);
    const win = window.open(objectURL);
    if (win) {
      win.onload = () => URL.revokeObjectURL(objectURL);
    }
  },

  /**
   * Method to convert the file to base64
   */
  toBase64(file, cb) {
    const fileReader = new FileReader();
    fileReader.readAsDataURL(file);
    fileReader.onload = (e: any) => {
      const base64Data = e.target.result.substring(e.target.result.indexOf('base64,') + 'base64,'.length);
      cb(base64Data);
    };
  },

  /**
   * Method to clear the input
   */
  clearInputImage(entity, elementRef, field, fieldContentType, idInput) {
    if (entity && field && fieldContentType) {
      if (Object.hasOwn(entity, field)) {
        entity[field] = null;
      }
      if (Object.hasOwn(entity, fieldContentType)) {
        entity[fieldContentType] = null;
      }
      if (elementRef && idInput && elementRef.nativeElement.querySelector(`#${idInput}`)) {
        elementRef.nativeElement.querySelector(`#${idInput}`).value = null;
      }
    }
  },

  endsWith(suffix, str) {
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
  },

  paddingSize(value) {
    if (this.endsWith('==', value)) {
      return 2;
    }
    if (this.endsWith('=', value)) {
      return 1;
    }
    return 0;
  },

  size(value) {
    return (value.length / 4) * 3 - this.paddingSize(value);
  },

  formatAsBytes(size) {
    return `${size.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ' ')} bytes`;
  },

  setFileData(event, entity, field, isImage) {
    if (event && event.target.files && event.target.files[0]) {
      const file = event.target.files[0];
      if (isImage && !/^image\//.test(file.type)) {
        return;
      }
      this.toBase64(file, base64Data => {
        entity[field] = base64Data;
        entity[`${field}ContentType`] = file.type;
      });
    }
  },

  /**
   * Method to download file
   */
  downloadFile(contentType, data, fileName) {
    const byteCharacters = atob(data);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
      byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    const blob = new Blob([byteArray], {
      type: contentType,
    });
    const tempLink = document.createElement('a');
    tempLink.href = window.URL.createObjectURL(blob);
    tempLink.download = fileName;
    tempLink.target = '_blank';
    tempLink.click();
  },

  /**
   * Method to parse header links
   */
  parseLinks(header) {
    const links = {};

    if ((header?.indexOf(',') ?? -1) === -1) {
      return links;
    }
    // Split parts by comma
    const parts = header.split(',');

    // Parse each part into a named link
    parts.forEach(p => {
      if (p.indexOf('>;') === -1) {
        return;
      }
      const section = p.split('>;');
      const url = section[0].replace(/<(.*)/, '$1').trim();
      const queryString = { page: null };
      url.replace(new RegExp(/([^?=&]+)(=([^&]*))?/g), ($0, $1, $2, $3) => {
        queryString[$1] = $3;
      });
      let page = queryString.page;
      if (typeof page === 'string') {
        page = parseInt(page, 10);
      }
      const name = section[1].replace(/rel="(.*)"/, '$1').trim();
      links[name] = page;
    });
    return links;
  },

  /**
   * Extracts the ID from a message string based on the template:
   * "... (ID: <ID_VALUE>) ..."
   *
   * @param message The message string to parse.
   * @returns The extracted ID string, or null if no ID is found or the format doesn't match.
   */
  extractIdFromMessage(message: string | undefined): string | null {
    const regex = /\(ID: ([a-zA-Z0-9]+)\)/;
    const match = message?.match(regex);

    if (match && match[1]) {
      return match[1];
    }

    return null;
  },

  /**
   * Extracts the PQRS title from a message string based on the template:
   * "PQRS '<PQRS_TITLE>' ..."
   *
   * @param message The message string to parse.
   * @returns The extracted PQRS title string, or null if no title is found or the format doesn't match.
   */
  extractPqrsTitle(message: string | undefined): string | null {
    const regex = /PQRS '(.*?)'/;
    const match = message?.match(regex);

    if (match && match[1]) {
      return match[1];
    }

    return null;
  },

  /**
   * Extracts the due date from a message string.
   * Assumes the date is the last parameter before the final period,
   * and is typically preceded by " is due on ".
   * Example: "... is due on YYYY-MM-DDTHH:MM."
   *
   * @param message The message string to parse.
   * @returns The extracted due date, or null if not found.
   */
  extractLastParameterBeforeDot(message: string): Date | null {
    const regex = /(\S+)\.$/;
    const match = message.match(regex);
    if (match && match[1]) {
      const dateString = match[1];
      const timestamp = Date.parse(dateString);

      if (isNaN(timestamp)) {
        return null;
      }

      return new Date(timestamp);
    }

    return null;
  },

  buildUrlToViewPqrs(message: string): string | null {
    const pqrsId = this.extractIdFromMessage(message);
    return `pqrs/${pqrsId}/view`;
  },

  /**
   * Retrieves the string value from a string-based enum given its key.
   *
   * @param enumObject The enum object itself (e.g., NotificationType).
   * @param key The string key of the enum member (e.g., "PQRS_DUE_DATE_REMINDER").
   * @param defaultValue An optional value to return if the key is not found in the enum.
   * @returns The string value associated with the key, or the defaultValue if the key is not found,
   *          or undefined if the key is not found and no defaultValue is provided.
   */
  getEnumValueByKey<TEnum extends Record<string, string>, K extends string>(enumObject: TEnum, key: K, defaultValue: string): string {
    if (Object.prototype.hasOwnProperty.call(enumObject, key)) {
      return enumObject[key as keyof TEnum];
    }
    return defaultValue;
  },
});

export default useDataUtils;
