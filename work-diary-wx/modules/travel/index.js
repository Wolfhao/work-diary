import config from './config.js';

Page({
    data: {
        guides: [],
        statusBarHeight: '0px'
    },

    onLoad() {
        const { statusBarHeight } = wx.getWindowInfo();
        this.setData({
            guides: config.guides,
            statusBarHeight: statusBarHeight + 'px'
        });
    },

    onBackTap() {
        wx.navigateBack();
    },

    onGuideTap() {
        wx.navigateTo({
            url: '/modules/travel/pingyao/index'
        });
    }
});
