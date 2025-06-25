import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { CheckCircle, Calendar, Clock, User as UserIcon, Download, Mail, QrCode, Shield } from 'lucide-react';
import { format, parseISO } from 'date-fns';
import QRCode from 'qrcode';
import type { Event, Participation } from '../types/database';
import { supabase } from '../lib/supabase';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { useAuth } from '../contexts/AuthContext';

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL;

export function BookingConfirmation() {
  const { participationId } = useParams<{ participationId: string }>();
  const { user, profile } = useAuth();
  const [participation, setParticipation] = useState<Participation | null>(null);
  const [event, setEvent] = useState<Event | null>(null);
  const [qrCodeUrl, setQrCodeUrl] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (participationId) {
      fetchParticipationDetails();
    }
  }, [participationId]);

  const fetchParticipationDetails = async () => {
    try {
      setLoading(true);
      
      // Get the access token from the current session
      const { data: { session } } = await supabase.auth.getSession();
      if (!session?.access_token) {
        throw new Error('No authentication token available');
      }
      
      // Fetch participation details directly using the new endpoint
      const participationResponse = await fetch(`${BACKEND_URL}/api/participates/${participationId}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${session.access_token}`,
        },
      });

      if (!participationResponse.ok) {
        if (participationResponse.status === 404) {
          throw new Error('Participation not found');
        }
        throw new Error(`HTTP error! status: ${participationResponse.status}`);
      }

      const participationData = await participationResponse.json();
      setParticipation(participationData);

      // Fetch event details
      const eventResponse = await fetch(`${BACKEND_URL}/api/events/${participationData.eventId}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${session.access_token}`,
        },
      });

      if (!eventResponse.ok) {
        throw new Error(`Failed to fetch event details: ${eventResponse.status}`);
      }

      const eventData = await eventResponse.json();
      setEvent(eventData);

      // Generate QR code
      const qrData = JSON.stringify({
        participationId: participationData.id,
        eventId: participationData.eventId,
        userId: participationData.userId,
        eventTitle: eventData.title,
        timestamp: new Date().toISOString(),
      });
      
      const qrCodeDataUrl = await QRCode.toDataURL(qrData, {
        width: 256,
        margin: 2,
        color: {
          dark: '#1f2937',
          light: '#ffffff',
        },
      });
      setQrCodeUrl(qrCodeDataUrl);

    } catch (error: any) {
      console.error('Error fetching participation details:', error);
      setError(error.message || 'Failed to load participation details.');
    } finally {
      setLoading(false);
    }
  };

  const downloadQRCode = () => {
    if (!qrCodeUrl) return;
    
    const link = document.createElement('a');
    link.download = `event-ticket-${participationId}.png`;
    link.href = qrCodeUrl;
    link.click();
  };

  const getRoleBadgeColor = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return 'bg-red-100 text-red-800';
      case 'ORGANIZER':
        return 'bg-purple-100 text-purple-800';
      default:
        return 'bg-blue-100 text-blue-800';
    }
  };

  const getGenderDisplay = (gender: string) => {
    switch (gender) {
      case 'MALE':
        return 'Male';
      case 'FEMALE':
        return 'Female';
      case 'OTHERS':
        return 'Other';
      default:
        return gender;
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error || !participation || !event) {
    return (
      <div className="max-w-2xl mx-auto text-center py-12">
        <div className="bg-red-50 border border-red-200 rounded-lg p-8">
          <h1 className="text-2xl font-bold text-red-900 mb-4">Registration Not Found</h1>
          <p className="text-red-700 mb-6">{error || 'The participation ID you provided is invalid or has expired.'}</p>
          <Link
            to="/dashboard"
            className="inline-flex items-center px-6 py-3 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors duration-200"
          >
            Back to Events
          </Link>
        </div>
      </div>
    );
  }

  if (!user || !profile) {
    return (
      <div className="max-w-2xl mx-auto text-center py-12">
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-8">
          <h1 className="text-2xl font-bold text-yellow-900 mb-4">Authentication Required</h1>
          <p className="text-yellow-700 mb-6">Please log in to view your registration details.</p>
          <Link
            to="/login"
            className="inline-flex items-center px-6 py-3 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors duration-200"
          >
            Log In
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      {/* Success Header */}
      <div className="text-center">
        <div className="mx-auto w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mb-4">
          <CheckCircle className="w-8 h-8 text-green-600" />
        </div>
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Registration Confirmed!</h1>
        <p className="text-gray-600">Your event registration has been successfully processed.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Registration Details */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-6">Registration Details</h2>
          
          <div className="space-y-4">
            <div className="flex items-center justify-between py-3 border-b border-gray-100">
              <span className="text-gray-600">Registration ID</span>
              <span className="font-mono font-semibold text-gray-900 text-sm">{participation.id}</span>
            </div>
            
            <div className="flex items-center justify-between py-3 border-b border-gray-100">
              <span className="text-gray-600">Status</span>
              <span className="bg-green-100 text-green-800 text-sm font-medium px-3 py-1 rounded-full">
                Confirmed
              </span>
            </div>
            
            <div className="flex items-center justify-between py-3 border-b border-gray-100">
              <span className="text-gray-600">Registered On</span>
              <span className="font-semibold text-gray-900">
                {participation.created_at ? format(parseISO(participation.created_at), 'MMM d, yyyy') : 'Recently'}
              </span>
            </div>
          </div>

          {/* Attendee Information */}
          <div className="mt-8">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Attendee Information</h3>
            <div className="bg-gray-50 rounded-lg p-4">
              <div className="flex items-center space-x-3">
                <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center">
                  <UserIcon className="w-6 h-6 text-blue-600" />
                </div>
                <div className="flex-1">
                  <div className="font-medium text-gray-900">
                    User {profile.id.slice(0, 8)}
                  </div>
                  <div className="text-sm text-gray-500 mb-2">
                    ID: {profile.id}
                  </div>
                  <div className="flex items-center space-x-2">
                    <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${getRoleBadgeColor(profile.role)}`}>
                      <Shield className="w-3 h-3 mr-1" />
                      {profile.role}
                    </span>
                    <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                      {getGenderDisplay(profile.gender)}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Event Details */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-6">Event Details</h2>
          
          <div className="space-y-6">
            <div>
              <h3 className="font-semibold text-gray-900 text-lg mb-2">{event.title}</h3>
              {event.description && (
                <p className="text-gray-600">{event.description}</p>
              )}
            </div>

            <div className="space-y-4">
              <div className="flex items-start space-x-3">
                <Calendar className="w-5 h-5 text-blue-600 mt-1" />
                <div>
                  <div className="font-medium text-gray-900">Start Time</div>
                  <div className="text-sm text-gray-500">
                    {format(parseISO(event.start_time), 'EEEE, MMMM d, yyyy \'at\' h:mm a')}
                  </div>
                </div>
              </div>

              <div className="flex items-start space-x-3">
                <Clock className="w-5 h-5 text-blue-600 mt-1" />
                <div>
                  <div className="font-medium text-gray-900">End Time</div>
                  <div className="text-sm text-gray-500">
                    {format(parseISO(event.end_time), 'EEEE, MMMM d, yyyy \'at\' h:mm a')}
                  </div>
                </div>
              </div>

              {event.capacity && (
                <div className="flex items-start space-x-3">
                  <UserIcon className="w-5 h-5 text-blue-600 mt-1" />
                  <div>
                    <div className="font-medium text-gray-900">Capacity</div>
                    <div className="text-sm text-gray-500">{event.capacity} attendees</div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* QR Code Section */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8">
        <div className="text-center">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Event Check-in</h2>
          <p className="text-gray-600 mb-6">Present this QR code at the event for quick check-in</p>
          
          <div className="inline-block p-4 bg-white border-2 border-gray-200 rounded-lg">
            <img
              src={qrCodeUrl}
              alt="Event QR Code"
              className="w-48 h-48 mx-auto"
            />
          </div>
          
          <div className="mt-6 flex flex-col sm:flex-row gap-4 justify-center">
            <button
              onClick={downloadQRCode}
              className="inline-flex items-center px-4 py-2 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors duration-200"
            >
              <Download className="w-4 h-4 mr-2" />
              Download QR Code
            </button>
            
            <Link
              to={`/events/${event.id}`}
              className="inline-flex items-center px-4 py-2 border border-gray-300 text-gray-700 font-medium rounded-lg hover:bg-gray-50 transition-colors duration-200"
            >
              View Event Details
            </Link>
          </div>
        </div>
      </div>

      {/* Next Steps */}
      <div className="bg-blue-50 border border-blue-200 rounded-xl p-8">
        <h2 className="text-xl font-semibold text-blue-900 mb-4">What's Next?</h2>
        <div className="space-y-3 text-blue-800">
          <div className="flex items-center space-x-2">
            <Mail className="w-5 h-5" />
            <span>Keep this confirmation for your records</span>
          </div>
          <div className="flex items-center space-x-2">
            <Calendar className="w-5 h-5" />
            <span>Add this event to your calendar to get reminders</span>
          </div>
          <div className="flex items-center space-x-2">
            <QrCode className="w-5 h-5" />
            <span>Save or screenshot the QR code for event check-in</span>
          </div>
        </div>
        
        <div className="mt-6 flex flex-col sm:flex-row gap-4">
          <Link
            to="/calendar"
            className="inline-flex items-center px-4 py-2 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors duration-200"
          >
            <Calendar className="w-4 h-4 mr-2" />
            View My Calendar
          </Link>
          
          <Link
            to="/dashboard"
            className="inline-flex items-center px-4 py-2 border border-blue-300 text-blue-700 font-medium rounded-lg hover:bg-blue-100 transition-colors duration-200"
          >
            Browse More Events
          </Link>
        </div>
      </div>
    </div>
  );
}
