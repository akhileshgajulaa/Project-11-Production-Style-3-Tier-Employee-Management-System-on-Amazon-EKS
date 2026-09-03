import axiosClient from './axiosClient';

export const getDashboardStatistics = () =>
  axiosClient.get('/dashboard/statistics').then((res) => res.data);
