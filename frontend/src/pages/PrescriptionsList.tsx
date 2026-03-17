import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext";
import { PrescriptionApi, Prescription, PrescriptionStatus, PatientApi, PatientProfile } from "../api";
import { HiPlus, HiPencil, HiCheckCircle, HiXCircle, HiClock, HiCalendar, HiUser } from "react-icons/hi";
import { MdLocalPharmacy } from "react-icons/md";

export default function PrescriptionsList() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedPrescription, setSelectedPrescription] = useState<Prescription | null>(null);
  const [patientProfiles, setPatientProfiles] = useState<Map<number, PatientProfile>>(new Map());

  useEffect(() => {
    fetchPrescriptions();
  }, []);

  useEffect(() => {
    if (user?.role === "DOCTOR" && prescriptions.length > 0) {
      fetchPatientProfiles();
    }
  }, [prescriptions, user?.role]);

  const fetchPatientProfiles = async () => {
    const uniquePatientIds = [...new Set(prescriptions.map(p => p.patientId))];
    const profilesMap = new Map<number, PatientProfile>(patientProfiles);
    
    const missingIds = uniquePatientIds.filter(id => !profilesMap.has(id));
    
    for (const patientId of missingIds) {
      try {
        const profile = await PatientApi.getProfileById(patientId);
        profilesMap.set(patientId, profile);
      } catch (err: any) {
        // Silently fail
      }
    }
    
    setPatientProfiles(profilesMap);
  };

  const fetchPrescriptions = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = user?.role === "DOCTOR" 
        ? await PrescriptionApi.getMyPrescriptions()
        : await PrescriptionApi.getPatientPrescriptions();
      setPrescriptions(data);
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to load prescriptions");
    } finally {
      setLoading(false);
    }
  };

  const handleActivate = async (id: number) => {
    if (!window.confirm("Are you sure you want to activate this prescription?")) {
      return;
    }

    try {
      await PrescriptionApi.activatePrescription(id);
      fetchPrescriptions();
    } catch (err: any) {
      alert(err.response?.data?.message || "Failed to activate prescription");
    }
  };

  const handleDiscontinue = async (id: number) => {
    if (!window.confirm("Are you sure you want to discontinue this prescription?")) {
      return;
    }

    try {
      await PrescriptionApi.discontinuePrescription(id);
      fetchPrescriptions();
    } catch (err: any) {
      alert(err.response?.data?.message || "Failed to discontinue prescription");
    }
  };

  const getStatusColor = (status: PrescriptionStatus) => {
    switch (status) {
      case "DRAFT":
        return "bg-gray-500/20 text-gray-300 border-gray-500";
      case "ACTIVE":
        return "bg-green-500/20 text-green-300 border-green-500";
      case "DISCONTINUED":
        return "bg-yellow-500/20 text-yellow-300 border-yellow-500";
      case "EXPIRED":
        return "bg-red-500/20 text-red-300 border-red-500";
      default:
        return "bg-gray-500/20 text-gray-300 border-gray-500";
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric" });
  };

  const isExpired = (validUntil: string) => {
    return new Date(validUntil) < new Date();
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-900 py-8">
        <div className="max-w-7xl mx-auto px-4">
          <div className="flex items-center justify-center min-h-[400px]">
            <div className="text-gray-400">Loading prescriptions...</div>
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
            <h1 className="text-3xl font-bold text-white mb-2">Prescriptions</h1>
            <p className="text-gray-400">
              {user?.role === "DOCTOR" 
                ? "Manage prescriptions for your patients"
                : "View your prescription history"}
            </p>
          </div>
          {user?.role === "DOCTOR" && (
            <button
              onClick={() => navigate("/prescriptions/create")}
              className="flex items-center space-x-2 px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              <HiPlus className="w-5 h-5" />
              <span>Create Prescription</span>
            </button>
          )}
        </div>

        {prescriptions.length === 0 ? (
          <div className="bg-gray-800 rounded-lg shadow-sm p-12 text-center border border-gray-700">
            <MdLocalPharmacy className="w-16 h-16 text-gray-600 mx-auto mb-4" />
            <p className="text-gray-400 text-lg mb-2">No prescriptions found</p>
            <p className="text-gray-500 text-sm">
              {user?.role === "DOCTOR" 
                ? "Create your first prescription to get started"
                : "You don't have any prescriptions yet"}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {prescriptions.map((prescription) => (
              <div
                key={prescription.id}
                className="bg-gray-800 rounded-lg shadow-sm p-6 border border-gray-700 hover:border-gray-600 transition-colors cursor-pointer"
                onClick={() => setSelectedPrescription(prescription)}
              >
                <div className="flex items-start justify-between mb-4">
                  <div className="flex-1">
                    <h3 className="text-lg font-semibold text-white mb-1">
                      {prescription.medicationName}
                    </h3>
                    {user?.role === "DOCTOR" ? (
                      <p className="text-sm text-gray-400">{prescription.patientName}</p>
                    ) : (
                      <p className="text-sm text-gray-400">{prescription.doctorName}</p>
                    )}
                  </div>
                  <span className={`px-3 py-1 rounded-full text-xs font-medium border ${getStatusColor(prescription.status)}`}>
                    {prescription.status}
                  </span>
                </div>

                <div className="space-y-2 mb-4">
                  <div className="flex items-center text-gray-400 text-sm">
                    <span className="font-medium text-gray-300 mr-2">Dosage:</span>
                    <span>{prescription.dosage}</span>
                  </div>
                  <div className="flex items-center text-gray-400 text-sm">
                    <span className="font-medium text-gray-300 mr-2">Frequency:</span>
                    <span>{prescription.frequency}</span>
                  </div>
                  <div className="flex items-center text-gray-400 text-sm">
                    <span className="font-medium text-gray-300 mr-2">Duration:</span>
                    <span>{prescription.duration}</span>
                  </div>
                  <div className="flex items-center text-gray-400 text-sm">
                    <HiCalendar className="w-4 h-4 mr-2" />
                    <span>Valid until: {formatDate(prescription.validUntil)}</span>
                    {isExpired(prescription.validUntil) && (
                      <span className="ml-2 text-red-400 text-xs">(Expired)</span>
                    )}
                  </div>
                </div>

                {prescription.instructions && (
                  <div className="mb-4 p-3 bg-gray-700/50 rounded-lg">
                    <p className="text-xs text-gray-300 line-clamp-2">{prescription.instructions}</p>
                  </div>
                )}

                {user?.role === "DOCTOR" && (
                  <div className="flex space-x-2 mt-4">
                    {prescription.status === "DRAFT" && (
                      <>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            navigate(`/prescriptions/${prescription.id}/edit`);
                          }}
                          className="flex-1 px-3 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center space-x-1 text-sm"
                        >
                          <HiPencil className="w-4 h-4" />
                          <span>Edit</span>
                        </button>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            handleActivate(prescription.id);
                          }}
                          className="flex-1 px-3 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors flex items-center justify-center space-x-1 text-sm"
                        >
                          <HiCheckCircle className="w-4 h-4" />
                          <span>Activate</span>
                        </button>
                      </>
                    )}
                    {prescription.status === "ACTIVE" && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDiscontinue(prescription.id);
                        }}
                        className="w-full px-3 py-2 bg-yellow-600 text-white rounded-lg hover:bg-yellow-700 transition-colors flex items-center justify-center space-x-1 text-sm"
                      >
                        <HiXCircle className="w-4 h-4" />
                        <span>Discontinue</span>
                      </button>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

        {/* Prescription Detail Modal */}
        {selectedPrescription && (
          <div
            className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
            onClick={() => setSelectedPrescription(null)}
          >
            <div
              className="bg-gray-800 rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto border border-gray-700"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="p-6">
                <div className="flex items-start justify-between mb-6">
                  <div>
                    <h2 className="text-2xl font-bold text-white mb-2">
                      {selectedPrescription.medicationName}
                    </h2>
                    <div className="flex items-center space-x-4 text-sm text-gray-400">
                      {user?.role === "DOCTOR" ? (
                        <div className="flex items-center">
                          <HiUser className="w-4 h-4 mr-2" />
                          <span>Patient: {selectedPrescription.patientName}</span>
                        </div>
                      ) : (
                        <div className="flex items-center">
                          <HiUser className="w-4 h-4 mr-2" />
                          <span>Doctor: {selectedPrescription.doctorName}</span>
                        </div>
                      )}
                    </div>
                  </div>
                  <span className={`px-3 py-1 rounded-full text-xs font-medium border ${getStatusColor(selectedPrescription.status)}`}>
                    {selectedPrescription.status}
                  </span>
                </div>

                <div className="space-y-4 mb-6">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="bg-gray-700/50 rounded-lg p-4">
                      <p className="text-xs text-gray-400 mb-1">Dosage</p>
                      <p className="text-white font-medium">{selectedPrescription.dosage}</p>
                    </div>
                    <div className="bg-gray-700/50 rounded-lg p-4">
                      <p className="text-xs text-gray-400 mb-1">Frequency</p>
                      <p className="text-white font-medium">{selectedPrescription.frequency}</p>
                    </div>
                    <div className="bg-gray-700/50 rounded-lg p-4">
                      <p className="text-xs text-gray-400 mb-1">Duration</p>
                      <p className="text-white font-medium">{selectedPrescription.duration}</p>
                    </div>
                    <div className="bg-gray-700/50 rounded-lg p-4">
                      <p className="text-xs text-gray-400 mb-1">Status</p>
                      <p className="text-white font-medium">{selectedPrescription.status}</p>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="flex items-center text-gray-400 text-sm">
                      <HiCalendar className="w-4 h-4 mr-2" />
                      <div>
                        <p className="text-xs text-gray-500">Issued</p>
                        <p className="text-white">{formatDate(selectedPrescription.issuedAt)}</p>
                      </div>
                    </div>
                    <div className="flex items-center text-gray-400 text-sm">
                      <HiClock className="w-4 h-4 mr-2" />
                      <div>
                        <p className="text-xs text-gray-500">Valid Until</p>
                        <p className={`${isExpired(selectedPrescription.validUntil) ? 'text-red-400' : 'text-white'}`}>
                          {formatDate(selectedPrescription.validUntil)}
                        </p>
                      </div>
                    </div>
                  </div>

                  {selectedPrescription.instructions && (
                    <div className="bg-gray-700/50 rounded-lg p-4">
                      <p className="text-xs text-gray-400 mb-2">Instructions</p>
                      <p className="text-white text-sm whitespace-pre-wrap">{selectedPrescription.instructions}</p>
                    </div>
                  )}
                </div>

                <div className="flex justify-end space-x-3">
                  <button
                    onClick={() => setSelectedPrescription(null)}
                    className="px-4 py-2 bg-gray-700 text-white rounded-lg hover:bg-gray-600 transition-colors"
                  >
                    Close
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

