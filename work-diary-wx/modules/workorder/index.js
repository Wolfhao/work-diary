Page({
    data: {
        activeTab: 'dashboard',
        statusBarHeight: '0px'
    },

    onLoad() {
        const { statusBarHeight } = wx.getWindowInfo();
        this.setData({ statusBarHeight: statusBarHeight + 'px' });
        this._firstShow = true; // 首次 onShow 时组件已自行加载，跳过
    },

    onShow() {
        if (this._firstShow) {
            this._firstShow = false;
            return;
        }
        // 从 release 页返回时，若当前在商单列表 tab，刷新列表
        if (this.data.activeTab === 'list') {
            const comp = this.selectComponent('#order-container');
            if (comp) comp.refresh();
        }
    },

    onBackTap() {
        wx.navigateBack();
    },

    onTabChange(e) {
        const tab = e.currentTarget.dataset.value;
        this.setData({ activeTab: tab });
        // 切换到商单列表 tab 时刷新
        if (tab === 'list') {
            const comp = this.selectComponent('#order-container');
            if (comp) comp.refresh();
        }
    },

    onAddTap() {
        wx.navigateTo({
            url: '/modules/workorder/release/index'
        });
    }
});
