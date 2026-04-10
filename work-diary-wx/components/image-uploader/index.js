import { uploadFile, getImageUrl } from '../../api/request';
import Toast from 'tdesign-miniprogram/toast/index';

Component({
  properties: {
    fileList: {
      type: Array,
      value: [],
      observer(newVal) {
        // 自身 emit change 导致的回调，跳过以防循环
        if (this._emitting) return;
        this.setData({ _files: newVal || [] });
      }
    },
    max: { type: Number, value: 5 },
    disabled: { type: Boolean, value: false }
  },

  data: {
    _files: []
  },

  methods: {
    onAdd(e) {
      const newFiles = e.detail.files || [];
      if (!newFiles.length) return;

      const baseIndex = this.data._files.length;

      // 先批量追加 loading 占位，让 TDesign 立即显示上传动效
      const placeholders = newFiles.map(file => ({ ...file, status: 'loading' }));
      this.setData({ _files: [...this.data._files, ...placeholders] });

      // 逐个上传
      newFiles.forEach((file, i) => {
        const idx = baseIndex + i;
        uploadFile(file.url)
          .then(storePath => {
            // 替换整个数组而非逐字段更新，确保 TDesign 检测到引用变化并重渲染
            const files = this.data._files.map((f, fi) =>
              fi === idx
                ? { ...f, status: 'done', url: getImageUrl(storePath), _storePath: storePath }
                : f
            );
            this.setData({ _files: files });
            this._emitChange();
          })
          .catch(err => {
            const files = this.data._files.map((f, fi) =>
              fi === idx ? { ...f, status: 'failed' } : f
            );
            this.setData({ _files: files });
            console.log(err);
            Toast({
              context: this,
              selector: '#t-toast',
              message: typeof err === 'string' ? err : '图片上传失败'
            });
          });
      });
    },

    onRemove(e) {
      const { index } = e.detail;
      const files = this.data._files.filter((_, i) => i !== index);
      this.setData({ _files: files });
      this._emitChange();
    },

    _emitChange() {
      this._emitting = true;
      this.triggerEvent('change', { fileList: this.data._files });
      // 下一个 tick 后重置标志，确保 observer 能响应真正的外部更新
      setTimeout(() => { this._emitting = false; }, 0);
    }
  }
});
