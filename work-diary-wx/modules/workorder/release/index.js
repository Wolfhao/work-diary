import { get, post, put, del, getImageUrl } from '../../../api/request';
import Toast from 'tdesign-miniprogram/toast/index';

Page({
    data: {
        isEdit: false,
        isViewMode: false,
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
            status: 10,
            imageUrls: [] // 仅存后端返回的相对/绝对路径数组
        },

        // TDesign upload 组件要求的数据格式
        fileList: [],

        // 状态配置
        statusPickerVisible: false,
        statusLabel: '待开工',
        statusOptions: [
            { label: '待开工', value: 10 },
            { label: '制作中', value: 20 },
            { label: '待结款', value: 30 },
            { label: '已完成', value: 40 }
        ]
    },

    onLoad(options) {
        const isViewMode = options.mode === 'view';
        if (isViewMode) {
            this.setData({ isViewMode: true });
            wx.setNavigationBarTitle({ title: '商单详情' });
        } else if (options.mode === 'edit') {
            wx.setNavigationBarTitle({ title: '编辑商单' });
        }
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

    // 4. 图片列表变更（由 image-uploader 组件回调）
    onFileListChange(e) {
        this.setData({ fileList: e.detail.fileList });
    },

    // 只读模式 → 跳转编辑
    onEditTap() {
        wx.redirectTo({
            url: `/modules/workorder/release/index?id=${this.data.formData.id}&mode=edit`
        });
    },

    // 5. 提交表单
    onSubmit() {
        // 校验必填项
        if (!this.data.formData.title) {
            return Toast({ context: this, selector: '#t-toast', message: '商单名称不能为空' });
        }

        // 收集已上传完成图片的存储路径（_storePath 由 image-uploader 组件统一设置）
        const validImages = this.data.fileList
            .filter(item => item.status === 'done' || !item.status)
            .map(item => item._storePath || '')
            .filter(Boolean);

        this.setData({ 'formData.imageUrls': validImages, submitting: true });

        const apiCall = this.data.isEdit
            ? put('/work-order', this.data.formData)
            : post('/work-order', this.data.formData);

        apiCall.then(() => {
            Toast({ context: this, selector: '#t-toast', message: '保存成功', theme: 'success' });
            setTimeout(() => {
                wx.navigateBack();
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
