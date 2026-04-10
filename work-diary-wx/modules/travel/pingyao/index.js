Page({
    data: {
        statusBarHeight: '0px'
    },

    onLoad() {
        const { statusBarHeight } = wx.getWindowInfo();
        this.setData({
            statusBarHeight: statusBarHeight + 'px'
        });
    },

    onBackTap() {
        wx.navigateBack();
    }
});
