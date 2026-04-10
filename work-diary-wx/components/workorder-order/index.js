import { get, post, put, doLogin } from '../../api/request';

Component({
  data: {
    activeTab: '0',
    searchValue: '',
    listData: [[], [], []],
    pages: [1, 1, 1],
    hasMore: [true, true, true],
    loading: false
  },

  lifetimes: {
    attached() {
      this.init();
    }
  },

  methods: {
    init() {
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
      // 每次切换 tab 都拉取最新数据
      this.refreshCurrentTab();
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

      if (tabIdx === 1) {
        param.statuses = [10, 20];
      }
      if (tabIdx === 2) {
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

    clearAllTabsCache() {
      this.setData({
        listData: [[], [], []],
        pages: [1, 1, 1],
        hasMore: [true, true, true]
      });
    },

    // 供父页面通过 selectComponent 调用
    refresh() {
      this.clearAllTabsCache();
      this.loadOrderList();
    },

    onOrderAction(e) {
      const { action, item } = e.detail;
      let confirmText = '';
      let payload = Object.assign({}, item);

      if (action === 'completeWork') {
        confirmText = '确定工作已全部交付并提交审核吗？';
        payload.status = 30;
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
              this.clearAllTabsCache();
              this.loadOrderList();
            }).catch(err => {
              wx.hideLoading();
            });
          }
        }
      });
    }
  }
});
