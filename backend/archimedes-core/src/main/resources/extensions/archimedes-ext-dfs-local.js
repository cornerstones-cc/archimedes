{
  getFileUrl(fileId) {
    if (!fileId || fileId === 'null') return ''
    if (fileId.startsWith('http')) {
      return fileId
    }
    return this.$api.config.download + '?file_id=' + fileId + '&uid=' + this.$service.uid
  },
  uploadFile(file) {
    const data = new FormData()
    data.append('file', file, file.name)
    return this.$api.uploadFile(this.$service.uid, data)
  }
}