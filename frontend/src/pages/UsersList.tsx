import React, { useState, useEffect } from "react";
import { useAuth } from "../AuthContext";
import { UserApi, UserInfo } from "../api";
import { HiUser, HiMail, HiShieldCheck } from "react-icons/hi";

export default function UsersList() {
  const { user } = useAuth();
  const [users, setUsers] = useState<UserInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<"ALL" | "DOCTOR" | "PATIENT">("ALL");

  useEffect(() => {
    if (user?.role === "DOCTOR") {
      fetchUsers();
    }
  }, [user]);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await UserApi.getAllUsers();
      setUsers(data);
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to load users");
    } finally {
      setLoading(false);
    }
  };

  const filteredUsers = users.filter(u => {
    if (filter === "ALL") return true;
    return u.role === filter;
  });

  const getRoleColor = (role: string) => {
    switch (role) {
      case "DOCTOR":
        return "bg-blue-500/20 text-blue-300 border-blue-500";
      case "PATIENT":
        return "bg-green-500/20 text-green-300 border-green-500";
      default:
        return "bg-gray-500/20 text-gray-300 border-gray-500";
    }
  };

  const getRoleIcon = (role: string) => {
    if (role === "DOCTOR") {
      return <HiShieldCheck className="w-5 h-5" />;
    }
    return <HiUser className="w-5 h-5" />;
  };

  if (user?.role !== "DOCTOR") {
    return (
      <div className="min-h-screen bg-gray-900 py-8">
        <div className="max-w-7xl mx-auto px-4">
          <div className="flex items-center justify-center min-h-[400px]">
            <div className="text-red-500">Access denied. Only doctors can view this page.</div>
          </div>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-900 py-8">
        <div className="max-w-7xl mx-auto px-4">
          <div className="flex items-center justify-center min-h-[400px]">
            <div className="text-gray-400">Loading users...</div>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-900 py-8">
        <div className="max-w-7xl mx-auto px-4">
          <div className="flex items-center justify-center min-h-[400px]">
            <div className="text-red-500">{error}</div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-900 py-8">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold text-white mb-2">All Users</h1>
            <p className="text-gray-400">View and manage all users in the system</p>
          </div>
          <div className="flex space-x-2">
            <button
              onClick={() => setFilter("ALL")}
              className={`px-4 py-2 rounded-lg transition-colors ${
                filter === "ALL"
                  ? "bg-blue-600 text-white"
                  : "bg-gray-700 text-gray-300 hover:bg-gray-600"
              }`}
            >
              All ({users.length})
            </button>
            <button
              onClick={() => setFilter("DOCTOR")}
              className={`px-4 py-2 rounded-lg transition-colors ${
                filter === "DOCTOR"
                  ? "bg-blue-600 text-white"
                  : "bg-gray-700 text-gray-300 hover:bg-gray-600"
              }`}
            >
              Doctors ({users.filter(u => u.role === "DOCTOR").length})
            </button>
            <button
              onClick={() => setFilter("PATIENT")}
              className={`px-4 py-2 rounded-lg transition-colors ${
                filter === "PATIENT"
                  ? "bg-blue-600 text-white"
                  : "bg-gray-700 text-gray-300 hover:bg-gray-600"
              }`}
            >
              Patients ({users.filter(u => u.role === "PATIENT").length})
            </button>
          </div>
        </div>

        <div className="bg-gray-800 rounded-lg shadow-sm border border-gray-700 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-700/50">
                <tr>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                    User ID
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                    Name
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                    Email
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                    Role
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                    Profile ID
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-700">
                {filteredUsers.map((userInfo) => (
                  <tr key={userInfo.userId} className="hover:bg-gray-700/50 transition-colors">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-300">
                      {userInfo.userId}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        <div className="flex-shrink-0 h-10 w-10 rounded-full bg-gray-700 flex items-center justify-center mr-3">
                          {getRoleIcon(userInfo.role)}
                        </div>
                        <div className="text-sm font-medium text-white">
                          {userInfo.firstName || userInfo.lastName
                            ? `${userInfo.firstName} ${userInfo.lastName}`.trim()
                            : "No name"}
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center text-sm text-gray-300">
                        <HiMail className="w-4 h-4 mr-2" />
                        {userInfo.email}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span
                        className={`px-3 py-1 rounded-full text-xs font-medium border ${getRoleColor(
                          userInfo.role
                        )}`}
                      >
                        {userInfo.role}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-400">
                      {userInfo.profileId || "N/A"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {filteredUsers.length === 0 && (
          <div className="bg-gray-800 rounded-lg shadow-sm p-12 text-center border border-gray-700 mt-6">
            <HiUser className="w-16 h-16 text-gray-600 mx-auto mb-4" />
            <p className="text-gray-400 text-lg mb-2">No users found</p>
            <p className="text-gray-500 text-sm">
              {filter === "ALL"
                ? "No users in the system"
                : `No ${filter.toLowerCase()}s found`}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

