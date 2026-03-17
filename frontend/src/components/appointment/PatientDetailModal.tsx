import React from "react";
import { PatientProfile } from "../../api";
import { HiX } from "react-icons/hi";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { getImageUrl } from "../../utils/imageUtils";

interface PatientDetailModalProps {
  patient: PatientProfile | null;
  onClose: () => void;
}

export default function PatientDetailModal({ patient, onClose }: PatientDetailModalProps) {
  if (!patient) {
    return null;
  }

  const fullName = `${patient.firstName} ${patient.lastName}`;
  const initials = `${patient.firstName?.[0] || ""}${patient.lastName?.[0] || ""}`;
  const profilePicture = getImageUrl(patient.profilePicture);

  return (
    <div 
      className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4"
      onClick={(e) => {
        if (e.target === e.currentTarget) {
          onClose();
        }
      }}
    >
      <Card 
        className="w-full max-w-2xl max-h-[90vh] overflow-y-auto bg-gray-800 border-gray-700 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-3 border-b border-gray-700">
          <CardTitle className="text-xl font-bold text-white">Patient Information</CardTitle>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-white transition-colors p-1.5 hover:bg-gray-700 rounded-lg"
          >
            <HiX className="w-5 h-5" />
          </button>
        </CardHeader>
        <CardContent className="pt-4 space-y-4">
          <div className="flex items-center space-x-3 pb-4 border-b border-gray-700">
            <Avatar className="w-14 h-14 border-2 border-gray-700 flex-shrink-0">
              <AvatarImage 
                src={profilePicture || undefined} 
                alt={fullName}
                className="object-cover"
              />
              <AvatarFallback className="bg-gradient-to-br from-blue-500 to-cyan-400 text-white font-bold text-base">
                {initials}
              </AvatarFallback>
            </Avatar>
            <div>
              <h2 className="text-lg font-bold text-white">{fullName}</h2>
              <p className="text-gray-400 text-xs">Patient Profile</p>
            </div>
          </div>

          <div className="space-y-6">
            {/* Personal Information Section */}
            <div className="space-y-4">
              <h3 className="text-lg font-bold text-white border-b border-gray-700 pb-2">Personal Information</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-1">
                  <label className="block text-sm font-medium text-gray-400 mb-1">First Name</label>
                  <p className="text-white font-medium">{patient.firstName || "Not provided"}</p>
                </div>

                <div className="space-y-1">
                  <label className="block text-sm font-medium text-gray-400 mb-1">Last Name</label>
                  <p className="text-white font-medium">{patient.lastName || "Not provided"}</p>
                </div>

                <div className="space-y-1">
                  <label className="block text-sm font-medium text-gray-400 mb-1">Date of Birth</label>
                  <p className="text-white font-medium">
                    {patient.dateOfBirth 
                      ? new Date(patient.dateOfBirth).toLocaleDateString("en-GB", {
                          day: "2-digit",
                          month: "2-digit",
                          year: "numeric"
                        }).replace(/\//g, "-")
                      : "Not provided"}
                  </p>
                </div>

                <div className="space-y-1">
                  <label className="block text-sm font-medium text-gray-400 mb-1">Gender</label>
                  <p className="text-white font-medium">{patient.gender || "Not specified"}</p>
                </div>
              </div>
            </div>

            {/* Medical Information Section */}
            <div className="space-y-4">
              <h3 className="text-lg font-bold text-white border-b border-gray-700 pb-2">Medical Information</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-1 md:col-span-2">
                  <label className="block text-sm font-medium text-gray-400 mb-1">Insurance Number</label>
                  <p className="text-white font-medium">{patient.insuranceNumber || "Not provided"}</p>
                </div>

                <div className="space-y-1 md:col-span-2">
                  <label className="block text-sm font-medium text-gray-400 mb-1">Medical Conditions</label>
                  <div className="p-3 bg-gray-700/50 rounded-lg">
                    <p className="text-white text-sm whitespace-pre-wrap">{patient.conditions || "None"}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

