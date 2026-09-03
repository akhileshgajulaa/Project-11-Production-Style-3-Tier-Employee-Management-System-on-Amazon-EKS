import axiosClient from './axiosClient';

export const getEmployees = (params) =>
  axiosClient.get('/employees', { params }).then((res) => res.data);

export const getEmployeeById = (id) =>
  axiosClient.get(`/employees/${id}`).then((res) => res.data);

export const createEmployee = (payload) =>
  axiosClient.post('/employees', payload).then((res) => res.data);

export const updateEmployee = (id, payload) =>
  axiosClient.put(`/employees/${id}`, payload).then((res) => res.data);

export const deactivateEmployee = (id) =>
  axiosClient.delete(`/employees/${id}`).then((res) => res.data);
