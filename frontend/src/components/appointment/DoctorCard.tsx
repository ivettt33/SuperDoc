import React from "react";
import { DoctorProfile } from "../../api";
import { HiStar, HiLocationMarker, HiBriefcase, HiXCircle } from "react-icons/hi";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { cn } from "@/lib/utils";

interface DoctorCardProps {
  doctor: DoctorProfile;
  isSelected: boolean;
  onSelect: (doctor: DoctorProfile) => void;
}

export default function DoctorCard({ doctor, isSelected, onSelect }: DoctorCardProps) {
  const fullName = `Dr. ${doctor.firstName} ${doctor.lastName}`;
  const initials = `${doctor.firstName?.[0] || ""}${doctor.lastName?.[0] || ""}`;

  return (
    <Card
      className={cn(
        "cursor-pointer transition-all duration-200 hover:scale-[1.02] text-left w-full",
        isSelected
          ? "ring-2 ring-blue-500 bg-blue-500/10 shadow-xl shadow-blue-500/30"
          : "border-gray-700 bg-gray-800/50 hover:border-gray-600 hover:bg-gray-800"
      )}
      onClick={() => onSelect(doctor)}
    >
      <CardContent className="p-5">
        <div className="flex items-start space-x-4">
          <Avatar className="w-20 h-20 border-2 border-gray-700 flex-shrink-0">
            <AvatarImage src={doctor.profilePhotoUrl} alt={fullName} />
            <AvatarFallback className="bg-gradient-to-br from-blue-500 to-cyan-400 text-white font-bold text-xl">
              {initials}
            </AvatarFallback>
          </Avatar>

          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between mb-2">
              <h3 className="font-semibold text-white text-lg truncate">{fullName}</h3>
              {isSelected && (
                <div className="flex-shrink-0 ml-2 w-3 h-3 bg-blue-500 rounded-full animate-pulse"></div>
              )}
            </div>

            {doctor.specialization && (
              <div className="flex items-center space-x-2 mb-2">
                <HiBriefcase className="w-4 h-4 text-gray-400 flex-shrink-0" />
                <Badge variant="secondary" className="text-sm">
                  {doctor.specialization}
                </Badge>
              </div>
            )}

            {doctor.clinicName && (
              <div className="flex items-center space-x-2 mb-3">
                <HiLocationMarker className="w-4 h-4 text-gray-400 flex-shrink-0" />
                <p className="text-xs text-gray-400 truncate">{doctor.clinicName}</p>
              </div>
            )}

            {doctor.isAbsent && (
              <div className="mb-3 p-2 bg-yellow-500/20 border border-yellow-500/50 rounded-lg flex items-center gap-2">
                <HiXCircle className="w-4 h-4 text-yellow-400 flex-shrink-0" />
                <p className="text-xs text-yellow-400 font-medium">
                  Dr. {doctor.firstName} {doctor.lastName} is currently out of office
                </p>
              </div>
            )}

            <div className="flex items-center space-x-4 mt-3 pt-3 border-t border-gray-700">
              {doctor.yearsOfExperience !== undefined && (
                <p className="text-xs text-gray-500">
                  {doctor.yearsOfExperience} {doctor.yearsOfExperience === 1 ? "year" : "years"} experience
                </p>
              )}
              <div className="flex items-center space-x-1">
                <HiStar className="w-4 h-4 text-yellow-400 fill-yellow-400" />
                <span className="text-xs text-gray-400 font-medium">5.0</span>
              </div>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}