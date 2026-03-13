import { get, post, put, doLogin } from '../../api/request';

Page({
    data: {
        activeTab: '0',
        searchValue: '',

        // 列表缓存 0:全部 1:进行中 2:待结款
        listData: [[], [], []],
        pages: [1, 1, 1],
        hasMore: [true, true, true],
        loading: false
    },

    onShow() {
        if (typeof this.getTabBar === 'function' && this.getTabBar()) {
            this.getTabBar().init();
        }

        const token = wx.getStorageSync('Authorization');
        if (token) {
            this.loadInitialData();
        } else {
            doLogin().then(() => {
                this.loadInitialData();
            });
        }
    },

    loadInitialData() {
        // 每次进入页面强制清空所有缓存，并重新加载当前 Tab 数据，确保数据最新
        this.clearAllTabsCache();
        this.loadOrderList();
    },

    onPullDownRefresh() {
        this.refreshCurrentTab().finally(() => {
            wx.stopPullDownRefresh();
        });
    },

    onReachBottom() {
        this.loadOrderList();
    },

    onTabsChange(e) {
        const newTab = e.detail.value;
        this.setData({ activeTab: newTab });

        const tabIdx = parseInt(newTab);
        if (this.data.listData[tabIdx].length === 0 && this.data.hasMore[tabIdx]) {
            this.loadOrderList();
        }
    },

    onSearchChange(e) {
        this.setData({ searchValue: e.detail.value });
        this.refreshCurrentTab();
    },

    refreshCurrentTab() {
        const tabIdx = parseInt(this.data.activeTab);
        const pages = this.data.pages;
        const hasMore = this.data.hasMore;
        const listData = this.data.listData;

        pages[tabIdx] = 1;
        hasMore[tabIdx] = true;
        listData[tabIdx] = [];

        this.setData({ pages, hasMore, listData });

        return this.loadOrderList();
    },

    loadOrderList() {
        const tabIdx = parseInt(this.data.activeTab);

        if (this.data.loading || !this.data.hasMore[tabIdx]) {
            return Promise.resolve();
        }

        this.setData({ loading: true });

        const param = {
            current: this.data.pages[tabIdx],
            size: 10
        };

        if (this.data.searchValue) {
            param.title = this.data.searchValue;
        }

        if (tabIdx === 1) { // 制作中
            param.statuses = [10, 20];
        }
        if (tabIdx === 2) { // 待结款
            param.status = 30;
        }

        return post('/work-order/page', param).then(res => {
            const listData = this.data.listData;
            const hasMore = this.data.hasMore;
            const pages = this.data.pages;

            const newRecords = res.records || [];
            listData[tabIdx] = listData[tabIdx].concat(newRecords);

            if (newRecords.length < 10) {
                hasMore[tabIdx] = false;
            } else {
                pages[tabIdx] = pages[tabIdx] + 1;
            }

            this.setData({
                listData,
                hasMore,
                pages
            });
        }).finally(() => {
            this.setData({ loading: false });
        });
    },

    // FAB 跳转记一笔
    onAddTap() {
        wx.navigateTo({
            url: '/pages/release/index'
        });
    },

    // 清空所有 Tab 的缓存（操作成功后调用，确保切换 Tab 时数据是最新的）
    clearAllTabsCache() {
        this.setData({
            listData: [[], [], []],
            pages: [1, 1, 1],
            hasMore: [true, true, true]
        });
    },

    // 处理列表中传来的业务操作按钮
    onOrderAction(e) {
        const { action, item } = e.detail;
        let confirmText = '';
        let payload = Object.assign({}, item); // 复制完整对象参数给接口

        if (action === 'completeWork') {
            confirmText = '确定工作已全部交付并提交审核吗？';
            payload.status = 30; // 变更为待结款
        } else if (action === 'recoverAdvance') {
            confirmText = '确定已收回垫付资金吗？';
            payload.isAdvanceRecovered = 1;
        } else if (action === 'receiveIncome') {
            confirmText = '确定已收到款项吗？';
            payload.isIncomeReceived = 1;
        }

        wx.showModal({
            title: '操作确认',
            content: confirmText,
            success: (modalRes) => {
                if (modalRes.confirm) {
                    wx.showLoading({ title: '处理中' });
                    put('/work-order', payload).then(res => {
                        wx.hideLoading();
                        wx.showToast({ title: '操作成功', icon: 'success' });
                        // 清空所有 Tab 的缓存，再重新加载当前 Tab
                        // 这样切换到其他 Tab 时也会重新拉取最新数据
                        this.clearAllTabsCache();
                        this.loadOrderList();
                    }).catch(err => {
                        wx.hideLoading();
                    });
                }
            }
        });
    }
});
