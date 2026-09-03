import axiosClient from './axiosClient';

export const getDepartments = () =>
  axiosClient.get('/departments').then((res) => res.data);

export const getDepartmentById = (id) =>
  axiosClient.get(`/departments/${id}`).then((res) => res.data);

export const createDepartment = (payload) =>
  axiosClient.post('/departments', payload).then((res) => res.data);

export const updateDepartment = (id, payload) =>
  axiosClient.put(`/departments/${id}`, payload).then((res) => res.data);

export const deleteDepartment = (id) =>
  axiosClient.delete(`/departments/${id}`).then((res) => res.data);
