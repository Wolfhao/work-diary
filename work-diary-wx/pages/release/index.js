import { get, post, put, del, getImageUrl } from '../../api/request';
import config from '../../api/request';
import Toast from 'tdesign-miniprogram/toast/index';

Page({
    data: {
        isEdit: false,
        submitting: false,

        // 表单对应的商单实体数据
        formData: {
            id: null,
            title: '',
            platform: '',
            advanceAmount: '',
            isAdvanceRecovered: 0,
            incomeAmount: '',
            isIncomeReceived: 0,
            description: '',
            status: 0,
            imageUrls: [] // 仅存后端返回的相对/绝对路径数组
        },

        // TDesign upload 组件要求的数据格式
        fileList: [],

        // 状态配置
        statusPickerVisible: false,
        statusLabel: '未开始',
        statusOptions: [
            { label: '未开始', value: 0 },
            { label: '进行中', value: 1 },
            { label: '已结款', value: 2 },
            { label: '已取消', value: 3 }
        ]
    },

    onLoad(options) {
        if (options.id) {
            this.setData({ isEdit: true, 'formData.id': options.id });
            this.loadOrderDetail(options.id);
        }
    },

    onShow() {
        if (typeof this.getTabBar === 'function' && this.getTabBar()) {
            this.getTabBar().init();
        }
    },

    // 1. 加载详情信息 (编辑模式)
    loadOrderDetail(id) {
        get(`/work-order/${id}`).then(res => {
            if (res) {
                // 回显基础数据
                this.setData({
                    formData: {
                        ...this.data.formData,
                        ...res
                    }
                });

                // 匹配状态 Label
                const matchStatus = this.data.statusOptions.find(opt => opt.value === res.status);
                if (matchStatus) {
                    this.setData({ statusLabel: matchStatus.label });
                }

                // 回显图片：将后端返回的存储路径转换为可直接显示的完整 URL
                if (res.imageUrls && res.imageUrls.length > 0) {
                    const files = res.imageUrls.map(url => ({
                        url: getImageUrl(url), // 转换为完整代理地址
                        _storePath: url,       // 保留原始存储路径，提交时用
                        type: 'image'
                    }));
                    this.setData({ fileList: files });
                }
            }
        });
    },

    // 2. 表单双向绑定处理器
    onTitleChange(e) { this.setData({ 'formData.title': e.detail.value }); },
    onPlatformChange(e) { this.setData({ 'formData.platform': e.detail.value }); },
    onAdvanceChange(e) { this.setData({ 'formData.advanceAmount': e.detail.value }); },
    onAdvanceRecoverChange(e) { this.setData({ 'formData.isAdvanceRecovered': e.detail.value ? 1 : 0 }); },
    onIncomeChange(e) { this.setData({ 'formData.incomeAmount': e.detail.value }); },
    onIncomeReceiveChange(e) { this.setData({ 'formData.isIncomeReceived': e.detail.value ? 1 : 0 }); },
    onDescChange(e) { this.setData({ 'formData.description': e.detail.value }); },

    // 3. 状态选择器
    onShowStatusPicker() { this.setData({ statusPickerVisible: true }); },
    onStatusPickerCancel() { this.setData({ statusPickerVisible: false }); },
    onStatusPickerChange(e) {
        const { value, label } = e.detail;
        this.setData({
            'formData.status': value[0],
            statusLabel: label[0],
            statusPickerVisible: false
        });
    },

    // 4. 图片上传与移除事件
    onAddMedia(e) {
        const { files } = e.detail;
        const token = wx.getStorageSync('Authorization');

        // 对每个新添加的文件执行真实上传调用
        files.forEach(file => {
            // 在 TDesign 的 fileList 中先追加一个上传中的占位状态
            const length = this.data.fileList.length;
            this.setData({
                [`fileList[${length}]`]: { ...file, status: 'loading' }
            });

            wx.uploadFile({
                url: config.BASE_URL + '/file/upload',
                filePath: file.url,
                name: 'file',
                header: { 'workDiaryAuthorization': token },
                success: (res) => {
                    const result = JSON.parse(res.data);
                    if (result.code === 200) {
                        // 上传成功：url 转成完整代理地址用于图片预览，_storePath 保留原始路径供提交时存库
                        this.setData({
                            [`fileList[${length}].status`]: 'done',
                            [`fileList[${length}].url`]: getImageUrl(result.data),
                            [`fileList[${length}]._storePath`]: result.data
                        });
                    } else {
                        // 失败
                        this.setData({ [`fileList[${length}].status`]: 'failed' });
                        Toast({ context: this, selector: '#t-toast', message: result.message || '上传失败' });
                    }
                },
                fail: () => {
                    this.setData({ [`fileList[${length}].status`]: 'failed' });
                    Toast({ context: this, selector: '#t-toast', message: '服务异常' });
                }
            });
        });
    },

    onRemoveMedia(e) {
        const { index } = e.detail;
        const { fileList } = this.data;
        fileList.splice(index, 1);
        this.setData({ fileList });
    },

    // 5. 提交表单
    onSubmit() {
        // 校验必填项
        if (!this.data.formData.title) {
            return Toast({ context: this, selector: '#t-toast', message: '商单名称不能为空' });
        }

        // 将已经传完成的 fileList 里的存储路径抽出来给 formData.imageUrls
        // - 回显的旧图：_storePath 保存了原始存储路径（如 /file/download?key=xxx）
        // - 新上传的图：url 直接就是服务端返回的存储路径
        const baseUrl = config.BASE_URL;
        const validImages = this.data.fileList
            .filter(item => item.status === 'done' || !item.status)
            .map(item => {
                if (item._storePath) return item._storePath; // 旧图直接取原始路径
                const url = item.url || '';
                // 新上传：如果 url 带有 baseUrl 前缀则去掉，保持一致的存储格式
                return url.startsWith(baseUrl) ? url.slice(baseUrl.length) : url;
            });

        this.setData({ 'formData.imageUrls': validImages, submitting: true });

        const apiCall = this.data.isEdit
            ? put('/work-order', this.data.formData)
            : post('/work-order', this.data.formData);

        apiCall.then(() => {
            Toast({ context: this, selector: '#t-toast', message: '保存成功', theme: 'success' });
            setTimeout(() => {
                wx.navigateBack({
                    fail: () => { wx.switchTab({ url: '/pages/home/index' }); }
                });
            }, 1200);
        }).finally(() => {
            this.setData({ submitting: false });
        });
    },

    // 6. 删除商单
    onDelete() {
        wx.showModal({
            title: '警告',
            content: '商单删除后无法恢复，是否确认？',
            confirmColor: '#e34d59',
            success: (res) => {
                if (res.confirm) {
                    del(`/work-order/${this.data.formData.id}`).then(() => {
                        Toast({ context: this, selector: '#t-toast', message: '已删除', theme: 'success' });
                        setTimeout(() => { wx.navigateBack(); }, 1200);
                    });
                }
            }
        });
    }
});
